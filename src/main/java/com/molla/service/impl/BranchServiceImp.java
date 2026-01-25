package com.molla.service.impl;

import com.molla.domain.UserRole;
import com.molla.service.BranchService;
import com.molla.exceptions.NotFoundException;
import com.molla.mapper.BranchMapper;
import com.molla.model.Branch;
import com.molla.model.Store;
import com.molla.model.User;
import com.molla.payload.dto.BranchDto;
import com.molla.repository.BranchRepository;
import com.molla.repository.StoreRepository;
import com.molla.service.UserService;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BranchServiceImp implements BranchService {
    private final BranchRepository branchRepository;
    private final StoreRepository storeRepository;
    private final UserService userService;
    @Override
    public BranchDto createBranch(BranchDto branchDto, User user) {
        User currentUser=userService.getCurrentUser();
        Store store = storeRepository.findByStoreAdminId(currentUser.getId());

        Branch branch = BranchMapper.toEntity(branchDto, store);
        Branch savedBranch = branchRepository.save(branch);
        return BranchMapper.toDto(savedBranch);
    }

    @Override
    @Cacheable(cacheNames = "branches", key = "#id")
    public BranchDto getBranchById(Long id) {
        Branch branch = branchRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Branch not found with id: " + id));
        return BranchMapper.toDto(branch);
    }

    @Override
    @Cacheable(cacheNames = "branchesByStore", key = "#storeId")
    public List<BranchDto> getBranchesByStoreId(Long storeId) {
        // 🔐 Authorization: Store admin can only view branches in their own store
        User currentUser = userService.getCurrentUser();
        if (currentUser != null && currentUser.getRole().equals(UserRole.ROLE_STORE_ADMIN)) {
            Store userStore = storeRepository.findByStoreAdminId(currentUser.getId());
            if (userStore == null || !userStore.getId().equals(storeId)) {
                throw new NotFoundException("You can only view branches in your own store");
            }
        }
        // Super admin can view any store's branches

        List<Branch> branches = branchRepository.findByStoreId(storeId);
        return branches.stream().map(BranchMapper::toDto).collect(Collectors.toList());
    }

    @Override
    public List<BranchDto> getAllBranches(User user) {
        if (user.getRole().equals(UserRole.ROLE_ADMIN)) {
            // Super admin: return all branches
            return branchRepository.findAll().stream()
                    .map(BranchMapper::toDto)
                    .collect(Collectors.toList());
        } else {
            // Store admin: return only branches in their store
            Store store = storeRepository.findByStoreAdminId(user.getId());
            if (store == null) {
                throw new NotFoundException("Store not found for this admin. Please create a store first.");
            }
            return getBranchesByStoreId(store.getId());
        }
    }

    @Override
    @CacheEvict(cacheNames = {"branches", "branchesByStore"}, allEntries = true)
    public BranchDto updateBranch(Long id, BranchDto branchDto, User user) {
        Branch existingBranch = branchRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Branch not found with id: " + id));
        existingBranch.setName(branchDto.getName());
        existingBranch.setPhone(branchDto.getPhone());
        existingBranch.setAddress(branchDto.getAddress());
        existingBranch.setEmail(branchDto.getEmail());
        existingBranch.setWorkingDays(branchDto.getWorkingDays());
        existingBranch.setOpenTime(branchDto.getOpenTime());
        existingBranch.setCloseTime(branchDto.getCloseTime());
        Branch savedBranch = branchRepository.save(existingBranch);
        return BranchMapper.toDto(savedBranch);

    }

    @Override
    @CacheEvict(cacheNames = {"branches", "branchesByStore"}, allEntries = true)
    public void deleteBranch(Long id) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Branch not found with id: " + id));
        branchRepository.delete(branch);
    }
}