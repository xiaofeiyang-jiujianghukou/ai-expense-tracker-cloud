package com.example.expense.category.controller;

import com.example.expense.category.dto.*;
import com.example.expense.category.entity.Category;
import com.example.expense.category.service.CategoryService;
import com.example.expense.common.ApiResponse;
import com.example.expense.common.enums.BillType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.example.expense.common.util.SecurityUtil;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    public ApiResponse<Void> create(@Valid @RequestBody CategoryRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        categoryService.create(request, userId);
        return ApiResponse.success();
    }

    @PostMapping("/list")
    public ApiResponse<List<CategoryVO>> list(@RequestBody CategoryListRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        BillType type = request.getType() != null ? BillType.valueOf(request.getType()) : null;
        List<Category> categories = categoryService.listByUser(userId, type);
        return ApiResponse.success(categories.stream().map(this::toVO).toList());
    }

    @GetMapping("/{id}")
    public ApiResponse<CategoryVO> getById(@PathVariable Long id ) {
        Long userId = SecurityUtil.getCurrentUserId();
        Category category = categoryService.findById(id);
        if (!category.getUserId().equals(userId)) {
            return ApiResponse.error(40301, "无权访问此资源");
        }
        return ApiResponse.success(toVO(category));
    }

    @PostMapping("/update")
    public ApiResponse<Void> update(@Valid @RequestBody CategoryUpdateRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        categoryService.update(request.getId(), request, userId);
        return ApiResponse.success();
    }

    @PostMapping("/delete")
    public ApiResponse<Void> delete(@Valid @RequestBody CategoryDeleteRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        categoryService.delete(request.getId(), userId);
        return ApiResponse.success();
    }

    private CategoryVO toVO(Category c) {
        return CategoryVO.builder()
                .id(c.getId()).name(c.getName()).type(c.getType())
                .createdTime(c.getCreatedTime()).build();
    }
}
