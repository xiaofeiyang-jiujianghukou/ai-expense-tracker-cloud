package com.example.expense.category.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.expense.category.entity.Category;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CategoryMapper extends BaseMapper<Category> {
}
