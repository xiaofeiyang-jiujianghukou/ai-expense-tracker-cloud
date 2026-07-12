package com.xiaofeiyang.expense.bill.api.dto;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Data;
@Data
@NoArgsConstructor @AllArgsConstructor
public class BillQueryMonthRequest { private int year; private int month; }
