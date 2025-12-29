package com.molla.service;

import com.molla.exceptions.UserException;
import com.molla.model.User;
import com.molla.payload.dto.BranchDto;

import java.util.List;

public interface BranchService {

    BranchDto createBranch(BranchDto branchDto, User user) throws UserException;
    BranchDto getBranchById(Long id);
    // List<BranchDto> getAllBranches();
    List<BranchDto> getBranchesByStoreId(Long storeId);
    BranchDto updateBranch(Long id, BranchDto branchDto, User user) throws UserException;
    void deleteBranch(Long id) throws UserException;
    
}
