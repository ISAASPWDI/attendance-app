package com.attendance.demo.dto.users;

import com.attendance.demo.entity.User;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** Director-initiated user creation — bypasses email verification since the director vouches for the account. */
@Getter @Setter
public class CreateUserDTO {

    @NotNull
    private String username;

    @NotNull
    private String password;

    @NotNull
    private String email;

    private String firstName;

    private String lastName;

    @NotNull
    private User.Role role;
}
