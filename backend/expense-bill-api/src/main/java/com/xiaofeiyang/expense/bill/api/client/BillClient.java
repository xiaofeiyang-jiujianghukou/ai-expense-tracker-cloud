package com.xiaofeiyang.expense.bill.api.client;

import com.xiaofeiyang.expense.bill.api.dto.BillDTO;
import com.xiaofeiyang.expense.bill.api.dto.BillQueryMonthRequest;
import com.xiaofeiyang.expense.bill.api.dto.BillQueryRangeRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(
        name = "bill-service",
        contextId = "bill-service-api",
        path = "/api/bills",
        url = "${bill-service-api.url:}"
)
public interface BillClient {

    @PostMapping("/query-range")
    List<BillDTO> queryByDateRange(@RequestBody BillQueryRangeRequest request);

    @PostMapping("/query-month")
    List<BillDTO> queryByMonth(@RequestBody BillQueryMonthRequest request);
}
