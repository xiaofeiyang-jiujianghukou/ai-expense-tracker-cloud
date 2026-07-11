package com.example.expense.bill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.expense.bill.entity.Bill;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BillMapper extends BaseMapper<Bill> {
}
