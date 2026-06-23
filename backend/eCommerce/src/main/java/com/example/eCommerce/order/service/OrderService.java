package com.example.eCommerce.order.service;

import com.example.eCommerce.order.dtos.OrderDto;
import com.example.eCommerce.order.entity.Order;
import com.example.eCommerce.order.enums.OrderStatus;
import com.example.eCommerce.order.orderMapper.OrderMapper;
import com.example.eCommerce.order.repository.OrderItemRepo;
import com.example.eCommerce.order.repository.OrderRepo;
import com.example.eCommerce.user.entity.Customer;
import com.example.eCommerce.user.repository.CustomerRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderService
{
    private final OrderRepo orderRepo;
    private final CustomerRepo customerRepo;
    private final OrderItemRepo orderItemRepo;

    @Transactional
    public OrderStatus saveOrderToCart(OrderDto orderRequestDTO)
    {
        Customer customer = customerRepo.getReferenceById(orderRequestDTO.getUserId());
        Order order = null;
        order = OrderMapper.mapToEntity(orderRequestDTO, customer);
        order.setOrderStatus(OrderStatus.SAVED_TO_CART);
        
        orderRepo.save(order);
        orderItemRepo.saveAll(orderRequestDTO.getOrderItems());

        return OrderStatus.SAVED_TO_CART;
    }
}
