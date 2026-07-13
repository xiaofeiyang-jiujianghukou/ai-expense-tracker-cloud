package com.xiaofeiyang.expense.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String email;
    private String password;
    private String nickname;
    private Integer status;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
