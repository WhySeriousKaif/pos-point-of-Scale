package com.molla.service.impl;

import com.molla.domain.OrderStatus;
import com.molla.domain.PaymentType;
import com.molla.mapper.OrderMapper;
import com.molla.model.Branch;
import com.molla.model.Customer;
import com.molla.model.Order;
import com.molla.model.OrderItem;
import com.molla.model.Product;
import com.molla.model.User;
import com.molla.payload.dto.OrderDto;
import com.molla.payload.dto.OrderItemDto;
import com.molla.repository.CustomerRepository;
import com.molla.repository.OrderRepository;
import com.molla.repository.ProductRepository;
import com.molla.service.OrderService;
import com.molla.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final UserService userService;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;

    @Override
    public OrderDto createOrder(OrderDto orderDto) throws Exception {
        // Get current user (cashier)
        User cashier = userService.getCurrentUser();
        Branch branch = cashier.getBranch();
        
        if (branch == null) {
            throw new Exception("Cashier is not assigned to any branch");
        }

        // Get customer if customerId is provided
        Customer customer = null;
        if (orderDto.getCustomerId() != null) {
            customer = customerRepository.findById(orderDto.getCustomerId())
                .orElseThrow(() -> new Exception("Customer not found"));
        }

        // Create order
        Order order = Order.builder()
            .branch(branch)
            .cashier(cashier)
            .customer(customer)
            .paymentType(orderDto.getPaymentType())
            .status(orderDto.getStatus() != null ? orderDto.getStatus() : OrderStatus.PENDING)
            .build();

        // Create order items
        if (orderDto.getOrderItems() == null || orderDto.getOrderItems().isEmpty()) {
            throw new Exception("Order must have at least one item");
        }

        List<OrderItem> orderItems = orderDto.getOrderItems().stream().map((OrderItemDto itemDto) -> {
            // Get product - check if productId is provided, otherwise use product.getId()
            final Long productId;
            if (itemDto.getProductId() != null) {
                productId = itemDto.getProductId();
            } else if (itemDto.getProduct() != null && itemDto.getProduct().getId() != null) {
                productId = itemDto.getProduct().getId();
            } else {
                throw new RuntimeException("Product ID is required for order item");
            }
            
            Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));
            
            Double itemPrice = itemDto.getPrice() != null ? itemDto.getPrice() : product.getPrice();
            Integer quantity = itemDto.getQuantity() != null ? itemDto.getQuantity() : 1;
            Double totalPrice = itemPrice * quantity;

            return OrderItem.builder()
                .product(product)
                .quantity(quantity)
                .price(itemPrice)
                .totalPrice(totalPrice)
                .order(order)
                .build();
        }).collect(Collectors.toList());

        // Calculate total amount
        double totalAmount = orderItems.stream()
            .mapToDouble(item -> item.getTotalPrice() != null ? item.getTotalPrice() : 0.0)
            .sum();
        
        order.setTotalAmount(totalAmount);
        order.setOrderItems(orderItems);

        // Save order
        Order savedOrder = orderRepository.save(order);
        return OrderMapper.toDto(savedOrder);
    }

    @Override
    public OrderDto updateOrder(Long id, OrderDto orderDto) throws Exception {
        Order order = orderRepository.findById(id)
            .orElseThrow(() -> new Exception("Order not found"));
        
        // Update order fields
        if (orderDto.getPaymentType() != null) {
            order.setPaymentType(orderDto.getPaymentType());
        }
        if (orderDto.getStatus() != null) {
            order.setStatus(orderDto.getStatus());
        }
        
        Order updatedOrder = orderRepository.save(order);
        return OrderMapper.toDto(updatedOrder);
    }

    @Override
    public void deleteOrder(Long id) throws Exception {
        Order order = orderRepository.findById(id)
            .orElseThrow(() -> new Exception("Order not found with id " + id));
        orderRepository.delete(order);
    }

    @Override
    public OrderDto getOrderById(Long id) throws Exception {
        Order order = orderRepository.findById(id)
            .orElseThrow(() -> new Exception("Order not found"));
        return OrderMapper.toDto(order);
    }

    @Override
    public List<OrderDto> getOrdersByBranch(Long branchId, Long customerId, Long cashierId, PaymentType paymentType, OrderStatus orderStatus) throws Exception {
        return orderRepository.findByBranchId(branchId).stream()
            .filter(order -> customerId == null || (order.getCustomer() != null && order.getCustomer().getId().equals(customerId)))
            .filter(order -> cashierId == null || (order.getCashier() != null && order.getCashier().getId().equals(cashierId)))
            .filter(order -> paymentType == null || order.getPaymentType() == paymentType)
            .filter(order -> orderStatus == null || order.getStatus() == orderStatus)
            .map(OrderMapper::toDto)
            .collect(Collectors.toList());
    }

    @Override
    public List<OrderDto> getOrdersByCashier(Long cashierId) throws Exception {
        return orderRepository.findByCashierId(cashierId).stream()
            .map(OrderMapper::toDto)
            .collect(Collectors.toList());
    }

    @Override
    public List<OrderDto> getTodayOrderbyBranch(Long branchId) throws Exception {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();
        
        return orderRepository.findByBranchIdAndCreatedAtBetween(branchId, start, end).stream()
            .map(OrderMapper::toDto)
            .collect(Collectors.toList());
    }

    @Override
    public List<OrderDto> getOrderByCustomerId(Long customerId) throws Exception {
        return orderRepository.findByCustomerId(customerId).stream()
            .map(OrderMapper::toDto)
            .collect(Collectors.toList());
    }

    @Override
    public List<OrderDto> getTop5RecentOrdersByBranchId(Long branchId) throws Exception {
        return orderRepository.findTop5ByBranchIdOrderByCreatedAtDesc(branchId).stream()
            .map(OrderMapper::toDto)
            .collect(Collectors.toList());
    }
}
