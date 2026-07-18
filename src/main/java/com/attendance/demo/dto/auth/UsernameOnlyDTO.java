package com.attendance.demo.dto.auth;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UsernameOnlyDTO {

    @NotNull
    public String username;
}
