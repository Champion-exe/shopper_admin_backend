package com.shopper.controller;

import com.shopper.common.Result;
import com.shopper.dto.PageResult;
import com.shopper.entity.Product;
import com.shopper.entity.ProductSku;
import com.shopper.entity.Review;
import com.shopper.repository.ReviewRepository;
import com.shopper.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/product")
public class ProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private ReviewRepository reviewRepository;

    /**
     * 商品分页列表（支持分类 + 关键词搜索）
     */
    @GetMapping("/list")
    public Result<PageResult<Product>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword) {
        return Result.success(productService.list(page, size, categoryId, keyword));
    }

    @GetMapping("/hot")
    public Result<List<Product>> hot() {
        return Result.success(productService.getHotProducts());
    }

    @GetMapping("/new")
    public Result<List<Product>> newProducts() {
        return Result.success(productService.getNewProducts());
    }

    @GetMapping("/seckill")
    public Result<List<Product>> seckill() {
        return Result.success(productService.getSeckillProducts());
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        Product product = productService.findById(id);
        if (product == null) {
            return Result.error(404, "商品不存在");
        }
        List<ProductSku> skus = productService.getSkus(id);
        List<Review> reviews = reviewRepository.findByProductId(id);

        Map<String, Object> data = new HashMap<>();
        data.put("product", product);
        data.put("skus", skus);
        data.put("reviews", reviews);
        return Result.success(data);
    }

    @GetMapping("/search")
    public Result<List<Product>> search(@RequestParam String keyword) {
        return Result.success(productService.list(1, 50, null, keyword).getList());
    }
}
