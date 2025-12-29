package com.molla.service.impl;

import com.molla.service.BranchService;
import com.molla.exceptions.UserException;
import com.molla.mapper.BranchMapper;
import com.molla.model.Branch;
import com.molla.model.Store;
import com.molla.model.User;
import com.molla.payload.dto.BranchDto;
import com.molla.repository.BranchRepository;
import com.molla.repository.StoreRepository;
import com.molla.service.UserService;

import lombok.RequiredArgsConstructor;
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
    public BranchDto createBranch(BranchDto branchDto, User user) throws UserException {
        User currentUser=userService.getCurrentUser();
        Store store = storeRepository.findByStoreAdminId(currentUser.getId());

        Branch branch = BranchMapper.toEntity(branchDto, store);
        Branch savedBranch = branchRepository.save(branch);
        return BranchMapper.toDto(savedBranch);
    }

    @Override
    public BranchDto getBranchById(Long id) {
        Branch branch = branchRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Branch not found"));
        return BranchMapper.toDto(branch);
        
      
    }

    @Override
    public List<BranchDto> getBranchesByStoreId(Long storeId) {
        List<Branch> branches = branchRepository.findByStoreId(storeId);
        return branches.stream().map(BranchMapper::toDto).collect(Collectors.toList());
    }

    @Override
    public BranchDto updateBranch(Long id, BranchDto branchDto, User user) throws UserException {
        Branch existingBranch = branchRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Branch not found"));
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
    public void deleteBranch(Long id) throws UserException {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Branch not found"));
        branchRepository.delete(branch);
    }
}