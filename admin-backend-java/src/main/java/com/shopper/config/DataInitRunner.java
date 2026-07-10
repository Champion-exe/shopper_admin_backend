package com.shopper.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopper.entity.*;
import com.shopper.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class DataInitRunner implements CommandLineRunner {

    @Autowired
    private ResourceLoader resourceLoader;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private ProductSkuRepository productSkuRepository;
    @Autowired
    private CartItemRepository cartItemRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private AddressRepository addressRepository;
    @Autowired
    private CouponRepository couponRepository;
    @Autowired
    private ReviewRepository reviewRepository;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() > 0) {
            return;
        }

        // 用户数据单独加载 — 明文密码 → BCrypt 加密
        Resource userRes = resourceLoader.getResource("classpath:data/users.json");
        if (userRes.exists()) {
            String json = StreamUtils.copyToString(userRes.getInputStream(), StandardCharsets.UTF_8);
            List<User> users = objectMapper.readValue(json, new TypeReference<List<User>>() {});
            users.forEach(u -> u.setPassword(passwordEncoder.encode(u.getPassword())));
            userRepository.saveAll(users);
        }

        loadData("data/categories.json", new TypeReference<List<Category>>() {}, categoryRepository);
        loadData("data/products.json", new TypeReference<List<Product>>() {}, productRepository);
        loadData("data/product-skus.json", new TypeReference<List<ProductSku>>() {}, productSkuRepository);
        loadData("data/cart-items.json", new TypeReference<List<CartItem>>() {}, cartItemRepository);
        loadData("data/orders.json", new TypeReference<List<Order>>() {}, orderRepository);
        loadData("data/addresses.json", new TypeReference<List<Address>>() {}, addressRepository);
        loadData("data/coupons.json", new TypeReference<List<Coupon>>() {}, couponRepository);
        loadData("data/reviews.json", new TypeReference<List<Review>>() {}, reviewRepository);
    }

    private <T> void loadData(String path, TypeReference<List<T>> typeRef,
                              org.springframework.data.jpa.repository.JpaRepository<T, ?> repo) throws Exception {
        Resource resource = resourceLoader.getResource("classpath:" + path);
        if (!resource.exists()) return;
        String json = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        List<T> list = objectMapper.readValue(json, typeRef);
        repo.saveAll(list);
    }
}
