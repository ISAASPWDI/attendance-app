package com.attendance.demo.specification;

import com.attendance.demo.dto.filter.AttendanceFilter;
import com.attendance.demo.entity.AttendanceRecord;
import com.attendance.demo.entity.User;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class AttendanceSpecification {

    public static Specification<AttendanceRecord> filter(AttendanceFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<AttendanceRecord, User> userJoin = root.join("user", JoinType.LEFT);

            if (filter.getTeacherName() != null && !filter.getTeacherName().isBlank()) {
                String term = "%" + filter.getTeacherName().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(userJoin.get("firstName")), term),
                        cb.like(cb.lower(userJoin.get("lastName")), term),
                        cb.like(cb.lower(userJoin.get("username")), term)
                ));
            }

            if (filter.getStatus() != null && !filter.getStatus().isBlank()) {
                predicates.add(cb.equal(root.get("status"),
                        AttendanceRecord.Status.valueOf(filter.getStatus())));
            }

            if (filter.getFromDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("date"), filter.getFromDate()));
            }

            if (filter.getToDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("date"), filter.getToDate()));
            }

            // Apply ordering only on result queries (not count queries for pagination)
            if (!Long.class.equals(query.getResultType())) {
                boolean desc = filter.getOrder() == null || filter.getOrder().isBlank()
                        || "desc".equalsIgnoreCase(filter.getOrder());
                Order order = switch (filter.getSortBy() != null ? filter.getSortBy().toLowerCase() : "date") {
                    case "teachername" -> desc
                            ? cb.desc(userJoin.get("firstName"))
                            : cb.asc(userJoin.get("firstName"));
                    case "status" -> desc
                            ? cb.desc(root.get("status"))
                            : cb.asc(root.get("status"));
                    default -> desc
                            ? cb.desc(root.get("date"))
                            : cb.asc(root.get("date"));
                };
                query.orderBy(order);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
