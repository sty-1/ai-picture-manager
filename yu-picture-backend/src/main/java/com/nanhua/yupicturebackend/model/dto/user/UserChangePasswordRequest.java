package com.nanhua.yupicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserChangePasswordRequest implements Serializable {

    private String oldPassword;

    private String newPassword;

    private static final long serialVersionUID = 1L;
}
