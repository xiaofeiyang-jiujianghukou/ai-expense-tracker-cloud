package com.xiaofeiyang.expense.category.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaofeiyang.expense.category.dto.CategoryRequest;
import com.xiaofeiyang.expense.category.entity.Category;
import com.xiaofeiyang.expense.category.mapper.CategoryMapper;
import com.xiaofeiyang.expense.framework.exception.BusinessException;
import com.xiaofeiyang.expense.framework.enums.BillType;
import com.xiaofeiyang.expense.framework.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryMapper categoryMapper;

    public void create(CategoryRequest request, Long userId) {
        Category category = new Category();
        category.setUserId(userId);
        category.setName(request.getName());
        category.setType(request.getType());
        categoryMapper.insert(category);
    }

    public List<Category> listByUser(Long userId, BillType type) {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<Category>()
                .eq(Category::getUserId, userId);
        if (type != null) {
            wrapper.eq(Category::getType, type.name());
        }
        wrapper.orderByAsc(Category::getCreatedTime);
        return categoryMapper.selectList(wrapper);
    }

    public void update(Long id, CategoryRequest request, Long userId) {
        Category category = findById(id);
        if (!category.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        category.setName(request.getName());
        category.setType(request.getType());
        categoryMapper.updateById(category);
    }

    public void delete(Long id, Long userId) {
        Category category = findById(id);
        if (!category.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        categoryMapper.deleteById(id);
    }

    public Category findById(Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
        }
        return category;
    }

    public void initDefaultCategories(Long userId) {
        for (BillType type : BillType.values()) {
            for (String name : type.getDefaultCategories()) {
                Category category = new Category();
                category.setUserId(userId);
                category.setName(name);
                category.setType(type.name());
                categoryMapper.insert(category);
            }
        }
    }
}
