package com.molla.service.impl;

import com.molla.domain.UserRole;
import com.molla.exceptions.BadRequestException;
import com.molla.exceptions.UserException;
import com.molla.mapper.UserMapper;
import com.molla.model.Branch;
import com.molla.model.Store;
import com.molla.model.User;
import com.molla.payload.dto.UserDto;
import com.molla.repository.BranchRepository;
import com.molla.repository.StoreRepository;
import com.molla.repository.UserRepository;
import com.molla.service.EmployeeService;
import com.molla.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final BranchRepository branchRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;

    @Override
    public UserDto createStoreEmployee(UserDto employee, Long storeId) throws UserException {
        Store store = storeRepository.findById(storeId).orElseThrow(() -> new RuntimeException("Store not found"));
        Branch branch = null; // branch is null if the employee is not a branch manager
        if (employee.getRole() == UserRole.ROLE_BRANCH_MANAGER) {
            if (employee.getBranchId() == null) {
                throw new UserException("Branch ID is required to create a branch manager");
            }
            branch = branchRepository.findById(employee.getBranchId())
                    .orElseThrow(() -> new RuntimeException("Branch not found"));
        }

        User user = UserMapper.toEntity(employee, passwordEncoder);
        user.setStore(store);
        user.setBranch(branch);

        User savedUser = userRepository.save(user);

        if (savedUser.getRole() == UserRole.ROLE_BRANCH_MANAGER && branch != null) {
            branch.setManager(savedUser);
            branchRepository.save(branch);
        }

        return UserMapper.toDto(savedUser);
    }

    @Override
    public UserDto createBranchEmployee(UserDto employeeDetails, Long branchId) throws UserException {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new RuntimeException("Branch not found"));

        // 🔐 Authorization: Super admin can add to any branch; store admin can only add
        // to branches in their store; branch manager can only add to their branch
        User currentUser = userService.getCurrentUser();
        if (currentUser.getRole().equals(UserRole.ROLE_STORE_ADMIN)) {
            // Store admin: verify branch belongs to their store
            Store userStore = storeRepository.findByStoreAdminId(currentUser.getId());
            if (userStore == null || !branch.getStore().getId().equals(userStore.getId())) {
                throw new BadRequestException("You can only add employees to branches in your store");
            }
        } else if (currentUser.getRole().equals(UserRole.ROLE_STORE_MANAGER)) {
            // Store manager: verify branch belongs to their store
            if (currentUser.getStore() == null || !branch.getStore().getId().equals(currentUser.getStore().getId())) {
                throw new BadRequestException("You can only add employees to branches in your store");
            }
        } else if (currentUser.getRole().equals(UserRole.ROLE_BRANCH_MANAGER)) {
            // Branch manager: verify branch is their own branch
            if (currentUser.getBranch() == null || !currentUser.getBranch().getId().equals(branchId)) {
                throw new BadRequestException("You can only add employees to your own branch");
            }
        } else if (!currentUser.getRole().equals(UserRole.ROLE_ADMIN)) {
            throw new BadRequestException(
                    "Only Super Admin, Store Admin, Store Manager or Branch Manager can add branch employees");
        }

        if (employeeDetails.getRole() == UserRole.ROLE_BRANCH_CASHIER
                || employeeDetails.getRole() == UserRole.ROLE_BRANCH_MANAGER) {
            User user = UserMapper.toEntity(employeeDetails, passwordEncoder);
            user.setBranch(branch);
            // Set store from branch
            user.setStore(branch.getStore());
            User savedUser = userRepository.save(user);
            if (employeeDetails.getRole() == UserRole.ROLE_BRANCH_MANAGER) {
                branch.setManager(savedUser);
                branchRepository.save(branch);
            }
            return UserMapper.toDto(savedUser);
        }

        throw new UserException("Invalid role for branch employee");
    }

    @Override
    public UserDto updateEmployee(UserDto employeeDetails, Long employeeId) throws UserException {
        User existingUser = userRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found with this id"));

        // 🔐 Authorization: Branch manager can only update employees in their branch
        User currentUser = userService.getCurrentUser();
        if (currentUser.getRole().equals(UserRole.ROLE_BRANCH_MANAGER)) {
            if (currentUser.getBranch() == null) {
                throw new BadRequestException("You are not assigned to a branch");
            }
            // If updating branchId, verify it's the manager's branch
            if (employeeDetails.getBranchId() != null
                    && !employeeDetails.getBranchId().equals(currentUser.getBranch().getId())) {
                throw new BadRequestException("You can only assign employees to your own branch");
            }
            // If employee already has a branch, verify it's the manager's branch
            if (existingUser.getBranch() != null
                    && !existingUser.getBranch().getId().equals(currentUser.getBranch().getId())) {
                throw new BadRequestException("You can only update employees in your own branch");
            }
        }

        existingUser.setFullName(employeeDetails.getFullName());
        existingUser.setEmail(employeeDetails.getEmail());
        existingUser.setPhone(employeeDetails.getPhone());
        existingUser.setRole(employeeDetails.getRole());

        // Only update password if provided
        if (employeeDetails.getPassword() != null && !employeeDetails.getPassword().isEmpty()) {
            existingUser.setPassword(passwordEncoder.encode(employeeDetails.getPassword()));
        }

        // Update branch if branchId is provided
        if (employeeDetails.getBranchId() != null) {
            Branch branch = branchRepository.findById(employeeDetails.getBranchId())
                    .orElseThrow(() -> new RuntimeException("Branch not found"));
            existingUser.setBranch(branch);
            // Update store from branch
            existingUser.setStore(branch.getStore());
        }

        User savedUser = userRepository.save(existingUser);
        return UserMapper.toDto(savedUser);
    }

    @Override
    public void deleteEmployee(Long employeeId) throws UserException {
        User existingUser = userRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        userRepository.delete(existingUser);
    }

    @Override
    public List<UserDto> findStoreEmployees(Long storeId, UserRole role) throws UserException {
        Store store = storeRepository.findById(storeId).orElseThrow(() -> new RuntimeException("Store not found"));
        return userRepository.findByStore(store)
                .stream()
                .filter(user -> role == null || user.getRole().equals(role)) //
                .map(UserMapper::toDto) // .map(user -> UserMapper.toDto(user))
                .collect(Collectors.toList());
    }

    @Override
    public List<UserDto> findBranchEmployees(Long branchId, UserRole role) throws UserException {
        branchRepository.findById(branchId)
                .orElseThrow(() -> new RuntimeException("Branch not found"));
        List<User> employees = userRepository.findByBranchId(branchId);
        return employees.stream()
                .filter(user -> role == null || user.getRole().equals(role))
                .map(UserMapper::toDto)
                .collect(Collectors.toList());
    }
}
