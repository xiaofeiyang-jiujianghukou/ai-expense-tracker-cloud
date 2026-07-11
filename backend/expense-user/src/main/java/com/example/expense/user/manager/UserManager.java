package com.example.expense.user.manager;

import com.example.expense.category.service.CategoryService;
import com.example.expense.security.JwtTokenProvider;
import com.example.expense.user.dto.*;
import com.example.expense.user.entity.User;
import com.example.expense.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UserManager {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final CategoryService categoryService;

    @Transactional
    public UserVO register(RegisterRequest request) {
        User user = userService.createUser(request);
        categoryService.initDefaultCategories(user.getId());
        return toVO(user);
    }

    public LoginResponse login(LoginRequest request) {
        User user = userService.verifyPassword(request);
        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail());
        return LoginResponse.builder()
                .token(token)
                .user(toVO(user))
                .build();
    }

    private UserVO toVO(User user) {
        return UserVO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .createdTime(user.getCreatedTime())
                .build();
    }
}
