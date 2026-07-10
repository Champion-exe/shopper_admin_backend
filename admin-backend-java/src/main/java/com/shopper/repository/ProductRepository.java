package com.shopper.repository;

import com.shopper.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /** 按分类查询 */
    List<Product> findByCategoryId(Long categoryId);

    /** 按分类分页查询 */
    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);

    /** 热门商品 */
    List<Product> findByIsHotTrue();

    /** 新品 */
    List<Product> findByIsNewTrue();

    /** 秒杀商品 */
    List<Product> findByIsSeckillTrue();

    /** 商家商品 */
    List<Product> findBySellerId(Long sellerId);

    /** 按名称模糊搜索 */
    List<Product> findByNameContaining(String keyword);

    /** 按名称模糊搜索 + 分页 */
    @Query("SELECT p FROM Product p WHERE (:keyword IS NULL OR p.name LIKE %:keyword%)")
    Page<Product> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /** 按分类 + 关键词 组合搜索 */
    @Query("SELECT p FROM Product p " +
           "WHERE (:categoryId IS NULL OR p.categoryId = :categoryId) " +
           "AND (:keyword IS NULL OR p.name LIKE %:keyword%)")
    Page<Product> search(@Param("categoryId") Long categoryId,
                         @Param("keyword") String keyword,
                         Pageable pageable);
}
