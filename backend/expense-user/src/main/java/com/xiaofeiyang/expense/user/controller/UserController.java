package com.xiaofeiyang.expense.user.controller;

import com.xiaofeiyang.expense.framework.ApiResponse;
import com.xiaofeiyang.expense.user.dto.LoginRequest;
import com.xiaofeiyang.expense.user.dto.LoginResponse;
import com.xiaofeiyang.expense.user.dto.RegisterRequest;
import com.xiaofeiyang.expense.user.dto.UserVO;
import com.xiaofeiyang.expense.user.manager.UserManager;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserManager userManager;

    @PostMapping("/register")
    public ApiResponse<UserVO> register(@Valid @RequestBody RegisterRequest request) {
        UserVO user = userManager.register(request);
        return ApiResponse.success(user);
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = userManager.login(request);
        return ApiResponse.success(response);
    }
}
