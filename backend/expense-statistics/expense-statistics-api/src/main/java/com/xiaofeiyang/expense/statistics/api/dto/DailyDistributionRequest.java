package com.xiaofeiyang.expense.statistics.api.dto;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Data;
@Data
@NoArgsConstructor @AllArgsConstructor
public class DailyDistributionRequest { private int year; private int month; }
