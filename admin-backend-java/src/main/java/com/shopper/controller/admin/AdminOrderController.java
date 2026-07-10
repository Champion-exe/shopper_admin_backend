package com.shopper.controller.admin;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopper.common.Result;
import com.shopper.dto.PageResult;
import com.shopper.entity.Order;
import com.shopper.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/order")
public class AdminOrderController {

    @Autowired
    private OrderRepository orderRepository;

    @GetMapping("/list")
    public Result<PageResult<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {

        Pageable pageable = PageRequest.of(page - 1, size);
        Page<Order> orderPage;
        if (status != null && !status.isEmpty()) {
            orderPage = orderRepository.findByStatus(status, pageable);
        } else {
            orderPage = orderRepository.findAll(pageable);
        }

        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> enriched = orderPage.getContent().stream().map(o -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", o.getId());
            map.put("orderNo", o.getOrderNo());
            map.put("userId", o.getUserId());
            map.put("status", o.getStatus());
            map.put("totalAmount", o.getTotalAmount());
            map.put("discountAmount", o.getDiscountAmount());
            map.put("payAmount", o.getPayAmount());
            map.put("paymentMethod", o.getPaymentMethod());
            map.put("addressId", o.getAddressId());
            map.put("consignee", o.getConsignee());
            map.put("phone", o.getPhone());
            map.put("address", o.getAddress());
            map.put("remark", o.getRemark());
            map.put("createTime", o.getCreateTime());
            map.put("paymentTime", o.getPaymentTime());
            map.put("deliveryTime", o.getDeliveryTime());
            map.put("confirmTime", o.getConfirmTime());
            // items 字符串 → 数组
            try {
                if (o.getItems() != null && !o.getItems().isBlank()) {
                    map.put("items", mapper.readValue(o.getItems(), new TypeReference<List<Map<String, Object>>>() {}));
                } else {
                    map.put("items", List.of());
                }
            } catch (Exception e) {
                map.put("items", List.of());
            }
            return map;
        }).toList();

        return Result.success(new PageResult<>(enriched, orderPage.getTotalElements(), page, size));
    }

    @PutMapping("/{id}/ship")
    public Result<Order> ship(@PathVariable Long id) {
        Order order = orderRepository.findById(id).orElse(null);
        if (order == null) return Result.error(404, "订单不存在");
        if (!"pending_ship".equals(order.getStatus())) {
            return Result.error(400, "只有待发货状态的订单才能发货");
        }
        order.setStatus("pending_receipt");
        order.setDeliveryTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        orderRepository.save(order);
        return Result.success(order);
    }

    @PutMapping("/{id}/cancel")
    public Result<Order> cancel(@PathVariable Long id) {
        Order order = orderRepository.findById(id).orElse(null);
        if (order == null) return Result.error(404, "订单不存在");
        if ("completed".equals(order.getStatus()) || "cancelled".equals(order.getStatus())) {
            return Result.error(400, "已完成/已取消的订单无法取消");
        }
        order.setStatus("cancelled");
        orderRepository.save(order);
        return Result.success(order);
    }
}
