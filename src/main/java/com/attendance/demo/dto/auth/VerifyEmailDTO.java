package com.attendance.demo.dto.auth;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class VerifyEmailDTO {

    @NotNull
    public String username;

    @NotNull
    @Pattern(regexp = "\\d{4}")
    public String code;
}
