package com.molla.service.impl;

import com.molla.mapper.InventoryMapper;
import com.molla.model.Branch;
import com.molla.model.Inventory;
import com.molla.model.Product;
import com.molla.payload.dto.InventoryDto;
import com.molla.repository.BranchRepository;
import com.molla.repository.InventoryRepository;
import com.molla.repository.ProductRepository;
import com.molla.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryServiceImp implements InventoryService {
    private final InventoryRepository inventoryRepository;
    private final BranchRepository branchRepository;
    private final ProductRepository productRepository;

    @Override
    public InventoryDto createInventory(InventoryDto inventoryDto) {
        // Validate required fields
        if (inventoryDto.getBranchId() == null) {
            throw new RuntimeException("Branch ID is required");
        }
        if (inventoryDto.getProductId() == null) {
            throw new RuntimeException("Product ID is required");
        }
        if (inventoryDto.getQuantity() == null) {
            throw new RuntimeException("Quantity is required");
        }

        Branch branch = branchRepository.findById(inventoryDto.getBranchId())
                .orElseThrow(() -> new RuntimeException("Branch not found"));

        Product product = productRepository.findById(inventoryDto.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        Inventory inventory = InventoryMapper.toEntity(inventoryDto, branch, product);
        Inventory savedInventory = inventoryRepository.save(inventory);
        return InventoryMapper.toDto(savedInventory);
    }

    @Override
    public InventoryDto updateInventory(InventoryDto inventoryDto, Long id) {
        Inventory existingInventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Inventory not found"));
        existingInventory.setQuantity(inventoryDto.getQuantity());
        Inventory updatedInventory = inventoryRepository.save(existingInventory);
        return InventoryMapper.toDto(updatedInventory);
    }

    @Override
    public void deleteInventory(Long id) {
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Inventory not found"));
        inventoryRepository.delete(inventory);
    }

    @Override
    public InventoryDto getInventoryByProductIdAndBranchId(Long productId, Long branchId) {
        Inventory inventory = inventoryRepository.findByProductIdAndBranchId(productId, branchId)
                .orElseThrow(() -> new RuntimeException("Inventory not found"));
        return InventoryMapper.toDto(inventory);
    }

    @Override
    public List<InventoryDto> getAllInventoriesByBranchId(Long branchId) {
        List<Inventory> inventories = inventoryRepository.findByBranchId(branchId);
        return inventories.stream()
                .map(InventoryMapper::toDto)
                .collect(Collectors.toList());
    }
}
