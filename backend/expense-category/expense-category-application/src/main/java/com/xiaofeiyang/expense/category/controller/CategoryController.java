package com.xiaofeiyang.expense.category.controller;

import com.xiaofeiyang.expense.category.api.dto.CategoryDTO;
import com.xiaofeiyang.expense.category.api.dto.CategoryDetailRequest;
import com.xiaofeiyang.expense.category.api.dto.InitDefaultsRequest;
import com.xiaofeiyang.expense.category.dto.*;
import com.xiaofeiyang.expense.category.entity.Category;
import com.xiaofeiyang.expense.category.service.CategoryService;
import com.xiaofeiyang.expense.framework.ApiResponse;
import com.xiaofeiyang.expense.framework.enums.BillType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.xiaofeiyang.expense.framework.util.SecurityUtil;
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

    @PostMapping("/feign/detail")
    public CategoryDTO feignDetail(@RequestBody CategoryDetailRequest request) {
        return toDTO(categoryService.findById(request.getId()));
    }

    @PostMapping("/detail")
    public CategoryDTO detail(@RequestBody CategoryDetailRequest request) {
        return toDTO(categoryService.findById(request.getId()));
    }

    @PostMapping("/init-defaults")
    public ApiResponse<Void> initDefaults(@RequestBody InitDefaultsRequest request) {
        categoryService.initDefaultCategories(request.getUserId());
        return ApiResponse.success();
    }

    private CategoryDTO toDTO(Category c) {
        return CategoryDTO.builder().id(c.getId()).userId(c.getUserId())
                .name(c.getName()).type(c.getType()).build();
    }

    private CategoryVO toVO(Category c) {
        return CategoryVO.builder()
                .id(c.getId()).name(c.getName()).type(c.getType())
                .createdTime(c.getCreatedTime()).build();
    }
}
