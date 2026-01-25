package com.molla.controllers;

import com.molla.domain.UserRole;
import com.molla.exceptions.BadRequestException;
import com.molla.exceptions.NotFoundException;
import com.molla.model.Branch;
import com.molla.model.Store;
import com.molla.model.User;
import com.molla.payload.dto.LoginRequest;
import com.molla.payload.dto.RegisterRequest;
import com.molla.payload.response.AuthResponse;
import com.molla.repository.BranchRepository;
import com.molla.repository.StoreRepository;
import com.molla.repository.UserRepository;
import com.molla.service.MailService;
import com.molla.util.JwtUtil;
import com.molla.mapper.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 🔐 Authentication Controller - Handles user registration and login
 * 
 * 👉 Purpose: Expose REST endpoints for user authentication
 * 🔹 Endpoints:
 * - POST /auth/signup → Register new user
 * - POST /auth/login → Authenticate existing user
 * 
 * 📌 Flow:
 * 1. Validate input using @Valid (Jakarta Validation)
 * 2. Check business rules (user exists, role allowed, etc.)
 * 3. Authenticate/create user
 * 4. Generate JWT token
 * 5. Return token + user details
 */
@Tag(name = "Authentication APIs", description = "User registration and login endpoints")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final BranchRepository branchRepository;
    private final StoreRepository storeRepository;

    // 📧 Optional email service (only active when spring.mail.host is configured)
    @Autowired(required = false)
    private MailService mailService;

    public AuthController(AuthenticationManager authManager,
                          JwtUtil jwtUtil,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          BranchRepository branchRepository,
                          StoreRepository storeRepository) {
        this.authManager = authManager;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.branchRepository = branchRepository;
        this.storeRepository = storeRepository;
    }

    /**
     * 📝 Register New User
     * 
     * 👉 Purpose: Create new user account and return JWT token
     * 🔹 Flow:
     * 1. Validate input (@Valid RegisterRequest)
     * 2. Check if user already exists → BadRequestException
     * 3. Check if trying to register as ADMIN → BadRequestException
     * 4. Hash password (BCrypt)
     * 5. Save user to database
     * 6. Send welcome email (optional, best-effort)
     * 7. Generate JWT token
     * 8. Return AuthResponse with token + user details
     * 
     * 📌 Security: Password is hashed before saving (never store plain passwords)
     */
    @Operation(summary = "Register a new user", 
               description = "Register a new user. Returns the created user with JWT token.")
    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        // 🔍 Step 1: Check if user already exists
        User existingUser = userRepository.findByEmail(request.getEmail());
        if (existingUser != null) {
            throw new BadRequestException("User already exists with email: " + request.getEmail());
        }

        // 🚫 Step 2: Prevent ADMIN registration (security measure)
        if (request.getRole().toString().equals("ROLE_ADMIN")) {
            throw new BadRequestException("Cannot register as ADMIN");
        }

        // 👤 Step 3: Create new user entity
        User newUser = new User();
        newUser.setEmail(request.getEmail());
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));  // 🔐 Hash password
        newUser.setRole(request.getRole());
        newUser.setFullName(request.getFullName());
        newUser.setPhone(request.getPhone());
        newUser.setLastLoginAt(LocalDateTime.now());
        newUser.setCreatedAt(LocalDateTime.now());
        newUser.setUpdatedAt(LocalDateTime.now());

        // 🏢 Step 3.5: Assign branch and store for branch managers
        Branch branchToUpdate = null;
        if (request.getRole().equals(UserRole.ROLE_BRANCH_MANAGER)) {
            if (request.getBranchId() != null) {
                Branch branch = branchRepository.findById(request.getBranchId())
                        .orElseThrow(() -> new BadRequestException("Branch not found with id: " + request.getBranchId()));
                newUser.setBranch(branch);
                newUser.setStore(branch.getStore());
                // Save branch reference for later (after user is saved)
                branchToUpdate = branch;
            } else if (request.getStoreId() != null) {
                // If only storeId provided, assign to store (branch will be set later)
                Store store = storeRepository.findById(request.getStoreId())
                        .orElseThrow(() -> new BadRequestException("Store not found with id: " + request.getStoreId()));
                newUser.setStore(store);
            }
        }

        // 💾 Step 4: Save user to database FIRST (must be saved before setting as branch manager)
        newUser = userRepository.save(newUser);

        // 🔗 Step 4.5: Set branch manager AFTER user is saved (to avoid TransientObjectException)
        if (branchToUpdate != null) {
            branchToUpdate.setManager(newUser);
            branchRepository.save(branchToUpdate);
        }

        // 📧 Step 5: Send welcome email (best-effort – failures are ignored so signup still works)
        if (mailService != null) {
            try {
                mailService.sendSimpleMail(
                        newUser.getEmail(),
                        "Welcome to Molla POS",
                        "Hi " + newUser.getFullName() + ",\n\n" +
                                "Your account has been created successfully.\n\n" +
                                "Role: " + newUser.getRole() + "\n\n" +
                                "Regards,\nMolla POS System"
                );
            } catch (Exception e) {
                // ⚠️ Email failure doesn't break signup
                System.out.println("Failed to send welcome email: " + e.getMessage());
            }
        }

        // 🎫 Step 6: Generate JWT token (email + role)
        String token = jwtUtil.generateToken(newUser.getEmail(), newUser.getRole().toString());

        // 📦 Step 7: Build response with token + user details
        AuthResponse authResponse = new AuthResponse();
        authResponse.setJwt(token);
        authResponse.setMessage("User registered successfully");
        authResponse.setUser(UserMapper.toDto(newUser));

        return new ResponseEntity<>(authResponse, HttpStatus.CREATED);
    }

    /**
     * 🔑 Login User
     * 
     * 👉 Purpose: Authenticate user and return JWT token
     * 🔹 Flow:
     * 1. Validate input (@Valid LoginRequest)
     * 2. Authenticate credentials using AuthenticationManager
     * 3. Load user from database
     * 4. Update last login timestamp
     * 5. Generate JWT token
     * 6. Return AuthResponse with token + user details
     * 
     * 📌 Security: AuthenticationManager handles password verification (BCrypt comparison)
     */
    @Operation(summary = "Login user", 
               description = "Authenticate user and receive JWT token. Returns token and user details.")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {

        // 🔐 Step 1: Authenticate user credentials
        // 👉 AuthenticationManager validates email + password against database
        // 🔹 Throws exception if credentials are invalid
        try {
            authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (Exception e) {
            // ❌ Invalid credentials → Return 400 Bad Request
            throw new BadRequestException("Invalid email or password");
        }

        // 👤 Step 2: Load user from database
        User user = userRepository.findByEmail(request.getEmail());
        if (user == null) {
            throw new NotFoundException("User not found: " + request.getEmail());
        }

        // ⏰ Step 3: Update last login timestamp
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // 🎫 Step 4: Generate JWT token (email + role)
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().toString());

        // 📦 Step 5: Build response with token + user details
        AuthResponse authResponse = new AuthResponse();
        authResponse.setJwt(token);
        authResponse.setMessage("Login successfully");
        authResponse.setUser(UserMapper.toDto(user));
        return new ResponseEntity<>(authResponse, HttpStatus.OK);
    }
}
