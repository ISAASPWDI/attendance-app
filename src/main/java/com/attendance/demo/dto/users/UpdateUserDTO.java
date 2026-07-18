package com.attendance.demo.dto.users;

import com.attendance.demo.entity.User;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UpdateUserDTO {
    private String firstName;
    private String lastName;
    /** Only honored when the requester is a DIRECTOR. */
    private User.Role role;
}
