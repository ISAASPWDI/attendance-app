package com.attendance.demo.specification;

import com.attendance.demo.dto.filter.AttendanceFilter;
import com.attendance.demo.entity.AttendanceRecord;
import com.attendance.demo.entity.User;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.DayOfWeek;
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

            if (filter.getDayOfWeek() != null && !filter.getDayOfWeek().isBlank()) {
                DayOfWeek dow = DayOfWeek.valueOf(filter.getDayOfWeek().toUpperCase());
                // Postgres date_part('isodow', date): 1=Monday..7=Sunday, matches DayOfWeek.getValue().
                Expression<Double> isoDow = cb.function("date_part", Double.class,
                        cb.literal("isodow"), root.get("date"));
                predicates.add(cb.equal(isoDow, (double) dow.getValue()));
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

    /** Restricts to a single user's own records — composed with {@link #filter} for "my attendance history" views. */
    public static Specification<AttendanceRecord> forUser(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
    }
}
