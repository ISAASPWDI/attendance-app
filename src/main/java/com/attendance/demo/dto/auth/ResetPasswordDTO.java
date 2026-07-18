package com.attendance.demo.dto.auth;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ResetPasswordDTO {

    @NotNull
    public String username;

    @NotNull
    @Pattern(regexp = "\\d{4}")
    public String code;

    @NotNull
    @Size(min = 8)
    public String newPassword;
}
