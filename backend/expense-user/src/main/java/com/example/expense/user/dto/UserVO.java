package com.example.expense.user.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserVO {
    private Long id;
    private String email;
    private String nickname;
    private LocalDateTime createdTime;
}
