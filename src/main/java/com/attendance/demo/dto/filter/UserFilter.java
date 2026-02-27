package com.attendance.demo.dto.filter;


import com.attendance.demo.dto.filter.enums.SortOrder;
import com.attendance.demo.dto.filter.enums.UserSortBy;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UserFilter {

    private String username;
    private String status;

    private LocalDate fromDate;
    private LocalDate toDate;

    private UserSortBy sortBy;
    private SortOrder order;


}