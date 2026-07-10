package com.shopper.service;

import com.shopper.dto.PageResult;
import com.shopper.entity.Product;
import com.shopper.entity.ProductSku;

import java.util.List;

public interface ProductService {

    /**
     * 分页查询商品（支持 categoryId + keyword 组合筛选）
     */
    PageResult<Product> list(int page, int size, Long categoryId, String keyword);

    /**
     * 按 ID 查商品
     */
    Product findById(Long id);

    /**
     * 查商品 SKU 列表
     */
    List<ProductSku> getSkus(Long productId);

    /**
     * 热门商品
     */
    List<Product> getHotProducts();

    /**
     * 新品
     */
    List<Product> getNewProducts();

    /**
     * 秒杀商品
     */
    List<Product> getSeckillProducts();
}
