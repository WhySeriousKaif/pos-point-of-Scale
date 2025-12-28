package com.molla.service.impl;

import com.molla.configuration.JwtProvider;
import com.molla.domain.UserRole;
import com.molla.exceptions.UserException;
import com.molla.mapper.UserMapper;
import com.molla.model.User;
import com.molla.payload.dto.UserDto;
import com.molla.payload.response.AuthResponse;
import com.molla.repository.UserRepository;
import com.molla.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImp implements AuthService {
    private final UserRepository userRepository;

    private  final PasswordEncoder passwordEncoder;

    private final JwtProvider jwtProvider;

    private  final CustomUserImplementation customUserImplementation;




    @Override
    public AuthResponse signUp(UserDto user) throws UserException {
        User existingUser = userRepository.findByEmail(user.getEmail());
        if (existingUser != null) {
            throw new UserException("User already exists");
        }
        if(user.getRole().equals(UserRole.ROLE_ADMIN)){
            throw new UserException("Cannot register as ADMIN");
        }

        User newUser=new User();
        newUser.setEmail(user.getEmail());
        newUser.setPassword(passwordEncoder.encode(user.getPassword()));
        newUser.setRole(user.getRole());
        newUser.setFullName(user.getFullName());
        newUser.setPhone(user.getPhone());
        newUser.setLastLoginAt(LocalDateTime.now());

        newUser.setCreatedAt(LocalDateTime.now());
        newUser.setUpdatedAt(LocalDateTime.now());

        userRepository.save(newUser);

        Authentication authentication=
                new UsernamePasswordAuthenticationToken(user.getEmail(),user.getPassword());

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        String jwt=jwtProvider.generateToken(authentication);

        AuthResponse authResponse=new AuthResponse();
        authResponse.setJwt(jwt);
        authResponse.setMessage("User registered successfully");
        authResponse.setUser(UserMapper.toDto(newUser));





        return authResponse;
    }

    @Override
    public AuthResponse login(UserDto user) {
        String email=user.getEmail();
        String password=user.getPassword();
        Authentication authentication=authenticate(email,password);
        String jwt=jwtProvider.generateToken(authentication);

        User foundUser=userRepository.findByEmail(email);


        foundUser.setLastLoginAt(LocalDateTime.now());

        userRepository.save(foundUser);

        AuthResponse authResponse=new AuthResponse();
        authResponse.setJwt(jwt);
        authResponse.setMessage("Login successfully");
        authResponse.setUser(UserMapper.toDto(foundUser));



        return authResponse;
    }

    private  Authentication authenticate(String email,String password){
        UserDetails userDetails=customUserImplementation.loadUserByUsername(email);
        if(!userDetails.getUsername().equals(email)){
            return  null;
        }

        if(passwordEncoder.matches(password,userDetails.getPassword())){
            return  new UsernamePasswordAuthenticationToken(
                    userDetails.getUsername(),
                    userDetails.getPassword(),
                    userDetails.getAuthorities()
            );
        }


        return  null;
    }
}
