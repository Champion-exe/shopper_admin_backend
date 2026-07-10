package com.shopper.service.impl;

import com.shopper.dto.PageResult;
import com.shopper.entity.Product;
import com.shopper.entity.ProductSku;
import com.shopper.repository.ProductRepository;
import com.shopper.repository.ProductSkuRepository;
import com.shopper.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductSkuRepository productSkuRepository;

    @Override
    public PageResult<Product> list(int page, int size, Long categoryId, String keyword) {
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<Product> productPage = productRepository.search(categoryId, keyword, pageable);
        return new PageResult<>(
                productPage.getContent(),
                productPage.getTotalElements(),
                page,
                size
        );
    }

    @Override
    public Product findById(Long id) {
        return productRepository.findById(id).orElse(null);
    }

    @Override
    public List<ProductSku> getSkus(Long productId) {
        return productSkuRepository.findByProductId(productId);
    }

    @Override
    public List<Product> getHotProducts() {
        return productRepository.findByIsHotTrue();
    }

    @Override
    public List<Product> getNewProducts() {
        return productRepository.findByIsNewTrue();
    }

    @Override
    public List<Product> getSeckillProducts() {
        return productRepository.findByIsSeckillTrue();
    }
}
