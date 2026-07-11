package com.example.expense.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.expense.common.exception.BusinessException;
import com.example.expense.common.exception.ErrorCode;
import com.example.expense.user.dto.LoginRequest;
import com.example.expense.user.dto.RegisterRequest;
import com.example.expense.user.entity.User;
import com.example.expense.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public User createUser(RegisterRequest request) {
        if (existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setStatus(1);

        userMapper.insert(user);
        return user;
    }

    public User findByEmail(String email) {
        return userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getEmail, email));
    }

    public boolean existsByEmail(String email) {
        return userMapper.exists(
                new LambdaQueryWrapper<User>().eq(User::getEmail, email));
    }

    public User findById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    public User verifyPassword(LoginRequest request) {
        User user = findByEmail(request.getEmail());
        if (user == null) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }
        return user;
    }
}
