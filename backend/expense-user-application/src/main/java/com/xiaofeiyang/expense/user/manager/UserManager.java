package com.xiaofeiyang.expense.user.manager;

import com.xiaofeiyang.expense.category.api.client.CategoryClient;
import com.xiaofeiyang.expense.category.api.dto.InitDefaultsRequest;
import com.xiaofeiyang.expense.framework.config.JwtTokenProvider;
import com.xiaofeiyang.expense.user.dto.*;
import com.xiaofeiyang.expense.user.entity.User;
import com.xiaofeiyang.expense.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserManager {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final CategoryClient categoryClient;

    /** NOTE: register() is NOT @Transactional — the Feign call to
     *  category-service is a separate HTTP request. In Phase 1 (shared DB)
     *  this is safe because both services connect to the same MySQL. */
    public UserVO register(RegisterRequest request) {
        User user = userService.createUser(request);
        categoryClient.initDefaultCategories(new InitDefaultsRequest(user.getId()));
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
