package com.shopper.controller.admin;

import com.shopper.common.Result;
import com.shopper.entity.Order;
import com.shopper.repository.OrderRepository;
import com.shopper.repository.ProductRepository;
import com.shopper.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminDashboardController {

    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private OrderRepository orderRepository;

    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard() {
        long productCount = productRepository.count();
        long userCount = userRepository.count();

        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        List<Order> allOrders = orderRepository.findAll();
        List<Order> todayOrders = allOrders.stream()
                .filter(o -> o.getCreateTime() != null && o.getCreateTime().startsWith(today))
                .toList();

        double todayRevenue = todayOrders.stream()
                .filter(o -> !"pending_payment".equals(o.getStatus()) && !"cancelled".equals(o.getStatus()))
                .mapToDouble(o -> o.getPayAmount() != null ? o.getPayAmount() : 0)
                .sum();

        Map<String, Object> data = new HashMap<>();
        data.put("productCount", productCount);
        data.put("userCount", userCount);
        data.put("todayOrders", todayOrders.size());
        data.put("todayRevenue", Math.round(todayRevenue * 100.0) / 100.0);
        return Result.success(data);
    }
}
