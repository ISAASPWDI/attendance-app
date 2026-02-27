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

            // filtro por username (LIKE %username%)
            if ( userFilter.getUsername() != null ) {
                Expression<String> field =  criteriaBuilder.lower(root.get("username"));
                String value = "%" + userFilter.getUsername().toLowerCase() + "%";
                predicates.add( criteriaBuilder.like(field, value));
            }

            // filtro por estado
            if ( userFilter.getStatus() != null){
                predicates.add( criteriaBuilder.equal(root.get("status"), userFilter.getStatus()));
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
