package com.attendance.demo.repository;

import com.attendance.demo.entity.PendingRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PendingRegistrationRepository extends JpaRepository<PendingRegistration, Long> {

    Optional<PendingRegistration> findByUsername(String username);

    boolean existsByUsername(String username);

    void deleteByUsername(String username);
}
