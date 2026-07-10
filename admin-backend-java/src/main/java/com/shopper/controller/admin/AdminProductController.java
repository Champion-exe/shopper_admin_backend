package com.shopper.controller.admin;

import com.shopper.common.Result;
import com.shopper.dto.PageResult;
import com.shopper.entity.Product;
import com.shopper.entity.ProductSku;
import com.shopper.repository.ProductRepository;
import com.shopper.repository.ProductSkuRepository;
import com.shopper.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/product")
public class AdminProductController {

    @Autowired
    private ProductService productService;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private ProductSkuRepository productSkuRepository;

    @GetMapping("/list")
    public Result<PageResult<Product>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId) {
        PageResult<Product> result = productService.list(page, size, categoryId, keyword);
        // 填充 minPrice / totalStock
        for (Product p : result.getList()) {
            List<ProductSku> skus = productSkuRepository.findByProductId(p.getId());
            if (!skus.isEmpty()) {
                p.setMinPrice(skus.stream().mapToDouble(ProductSku::getPrice).min().orElse(0));
                p.setTotalStock(skus.stream().mapToInt(ProductSku::getStock).sum());
            }
        }
        return Result.success(result);
    }

    @PostMapping("/save")
    public Result<Product> save(@RequestBody Map<String, Object> body) {
        Product product;
        Object idObj = body.get("id");
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        if (idObj != null) {
            // 编辑
            long id = Long.parseLong(idObj.toString());
            product = productRepository.findById(id).orElse(null);
            if (product == null) return Result.error(404, "商品不存在");
        } else {
            // 新增
            product = new Product();
            product.setId(productRepository.count() > 0
                    ? productRepository.findAll().stream().mapToLong(Product::getId).max().getAsLong() + 1
                    : 10001);
            product.setSales(0);
            product.setCreateTime(LocalDateTime.now());
        }

        if (body.containsKey("name")) product.setName((String) body.get("name"));
        if (body.containsKey("description")) product.setDescription((String) body.get("description"));
        if (body.containsKey("brand")) product.setBrand((String) body.get("brand"));
        if (body.containsKey("categoryId")) product.setCategoryId(Long.parseLong(body.get("categoryId").toString()));
        if (body.containsKey("sellerId")) product.setSellerId(Long.parseLong(body.get("sellerId").toString()));
        if (body.containsKey("mainImage")) product.setMainImage((String) body.get("mainImage"));
        if (body.containsKey("rating")) product.setRating(Double.parseDouble(body.get("rating").toString()));
        if (body.containsKey("isHot")) product.setIsHot((Boolean) body.get("isHot"));
        if (body.containsKey("isNew")) product.setIsNew((Boolean) body.get("isNew"));
        if (body.containsKey("isSeckill")) product.setIsSeckill((Boolean) body.get("isSeckill"));

        productRepository.save(product);
        return Result.success(product);
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        if (!productRepository.existsById(id)) {
            return Result.error(404, "商品不存在");
        }
        // 删除关联 SKU
        productSkuRepository.findByProductId(id).forEach(sku -> productSkuRepository.delete(sku));
        productRepository.deleteById(id);
        return Result.success("商品已删除");
    }
}
