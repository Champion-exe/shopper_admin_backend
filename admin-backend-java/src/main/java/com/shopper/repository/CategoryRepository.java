package com.shopper.repository;

import com.shopper.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /** 按父级ID查询子分类，按排序号升序 */
    List<Category> findByParentIdOrderBySortOrderAsc(Long parentId);

    /** 查询所有根分类（parentId = 0） */
    List<Category> findByParentId(Long parentId);
}
