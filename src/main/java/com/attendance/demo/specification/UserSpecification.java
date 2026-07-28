package com.attendance.demo.specification;

import com.attendance.demo.dto.filter.UserFilter;
import com.attendance.demo.entity.AttendanceRecord;
import com.attendance.demo.entity.User;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;


public class UserSpecification {

    public static Specification<User> filter (UserFilter userFilter) {
        return ( root, query, criteriaBuilder) -> {

//            query.distinct(true);
            Join<User, AttendanceRecord> attendanceRecordJoin = root.join("attendanceRecords", JoinType.LEFT);
            List<Predicate> predicates = new ArrayList<>();

            // filtro por username, nombre o apellido (LIKE %texto%)
            if ( userFilter.getUsername() != null && !userFilter.getUsername().isBlank() ) {
                String value = "%" + userFilter.getUsername().toLowerCase() + "%";
                predicates.add( criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("username")), value),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("firstName")), value),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("lastName")), value)
                ));
            }

            // filtro por rol
            if ( userFilter.getRole() != null && !userFilter.getRole().isBlank() ) {
                predicates.add( criteriaBuilder.equal(root.get("role"), User.Role.valueOf(userFilter.getRole())));
            }

            // filtro por fecha desde
            if ( userFilter.getFromDate() != null ){
                predicates.add( criteriaBuilder.greaterThanOrEqualTo(attendanceRecordJoin.get("date"), userFilter.getFromDate()));
            }
            // filtro or fecha hasta
            if ( userFilter.getToDate() != null ) {
                predicates.add( criteriaBuilder.lessThanOrEqualTo( attendanceRecordJoin.get("date"), userFilter.getToDate()));
            }


            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
