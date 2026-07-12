package com.xiaofeiyang.expense.category.api.client;

import com.xiaofeiyang.expense.category.api.dto.CategoryDTO;
import com.xiaofeiyang.expense.category.api.dto.CategoryDetailRequest;
import com.xiaofeiyang.expense.category.api.dto.CategoryListRequest;
import com.xiaofeiyang.expense.category.api.dto.InitDefaultsRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(
        name = "category-service",
        contextId = "category-service-api",
        path = "/api/categories",
        url = "${category-service-api.url:}"
)
public interface CategoryClient {

    @PostMapping("/list")
    List<CategoryDTO> listByUser(@RequestBody CategoryListRequest request);

    @PostMapping("/detail")
    CategoryDTO findById(@RequestBody CategoryDetailRequest request);

    @PostMapping("/init-defaults")
    void initDefaultCategories(@RequestBody InitDefaultsRequest request);
}
