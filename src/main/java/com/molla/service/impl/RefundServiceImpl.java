package com.molla.service.impl;

import com.molla.mapper.RefundMapper;
import com.molla.model.Branch;
import com.molla.model.Order;
import com.molla.model.Refund;
import com.molla.model.ShiftReport;
import com.molla.model.User;
import com.molla.payload.dto.RefundDto;
import com.molla.repository.BranchRepository;
import com.molla.repository.OrderRepository;
import com.molla.repository.RefundRepository;
import com.molla.repository.ShiftReportRepository;
import com.molla.service.RefundService;
import com.molla.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RefundServiceImpl implements RefundService {
    
    private final RefundRepository refundRepository;
    private final OrderRepository orderRepository;
    private final UserService userService;
    private final BranchRepository branchRepository;
    private final ShiftReportRepository shiftReportRepository;

    @Override
    public RefundDto createRefund(RefundDto refundDto) throws Exception {
        // Get cashier - use cashier from DTO if provided, otherwise use current user
        User cashier;
        try {
            if (refundDto.getCashier() != null && refundDto.getCashier().getId() != null) {
                cashier = userService.getUserById(refundDto.getCashier().getId());
            } else {
                cashier = userService.getCurrentUser();
            }
        } catch (Exception e) {
            // No authenticated user - use cashier from DTO or order's cashier
            if (refundDto.getCashier() != null && refundDto.getCashier().getId() != null) {
                cashier = userService.getUserById(refundDto.getCashier().getId());
            } else {
                // Get cashier from order
                Order tempOrder = orderRepository.findById(refundDto.getOrderId()).orElse(null);
                if (tempOrder != null && tempOrder.getCashier() != null) {
                    cashier = tempOrder.getCashier();
                } else {
                    cashier = userService.getUserById(1L); // Default for testing
                }
            }
        }
        
        // Get order
        Order order = orderRepository.findById(refundDto.getOrderId())
            .orElseThrow(() -> new Exception("Order not found"));
        
        // Get branch - use branch from DTO or from order
        Branch branch;
        if (refundDto.getBranch() != null && refundDto.getBranch().getId() != null) {
            branch = branchRepository.findById(refundDto.getBranch().getId())
                .orElse(order.getBranch());
        } else {
            branch = order.getBranch();
        }
        
        if (branch == null) {
            throw new Exception("Order does not have an associated branch");
        }

        // Get shift report if provided
        ShiftReport shiftReport = null;
        if (refundDto.getShiftReportId() != null) {
            try {
                shiftReport = shiftReportRepository.findById(refundDto.getShiftReportId())
                    .orElse(null);
            } catch (Exception e) {
                // Shift report not required, continue without it
            }
        }

        // Create refund
        Refund createdRefund = Refund.builder()
            .order(order)
            .cashier(cashier)
            .branch(branch)
            .reason(refundDto.getReason())
            .amount(refundDto.getAmount())
            .paymentType(refundDto.getPaymentType())
            .shiftReport(shiftReport)
            .createdAt(LocalDateTime.now())
            .build();

        // Save refund
        Refund savedRefund = refundRepository.save(createdRefund);
        
        return RefundMapper.toDto(savedRefund);
    }

    @Override
    public List<RefundDto> getAllRefunds() throws Exception {
        return refundRepository.findAll().stream()
            .map(RefundMapper::toDto)
            .collect(Collectors.toList());
    }

    @Override
    public List<RefundDto> getRefundByCashier(Long cashierId) throws Exception {
        return refundRepository.findByCashierId(cashierId).stream()
            .map(RefundMapper::toDto)
            .collect(Collectors.toList());
    }

    @Override
    public List<RefundDto> getRefundByShiftReport(Long shiftReportId) throws Exception {
        return refundRepository.findByShiftReportId(shiftReportId).stream()
            .map(RefundMapper::toDto)
            .collect(Collectors.toList());
    }

    @Override
    public List<RefundDto> getRefundByCashierAndDateRange(Long cashierId, LocalDateTime startDate, LocalDateTime endDate) throws Exception {
        User cashier = userService.getUserById(cashierId);
        return refundRepository.findByCashierAndCreatedAtBetween(cashier, startDate, endDate).stream()
            .map(RefundMapper::toDto)
            .collect(Collectors.toList());
    }

    @Override
    public List<RefundDto> getRefundByBranch(Long branchId) throws Exception {
        return refundRepository.findByBranchId(branchId).stream()
            .map(RefundMapper::toDto)
            .collect(Collectors.toList());
    }

    @Override
    public RefundDto getRefundById(Long refundId) throws Exception {
        return refundRepository.findById(refundId)
            .map(RefundMapper::toDto)
            .orElseThrow(() -> new Exception("Refund not found"));
    }

    @Override
    public void deleteRefund(Long refundId) throws Exception {
        // Check if refund exists
        this.getRefundById(refundId);
        refundRepository.deleteById(refundId);
    }
}

