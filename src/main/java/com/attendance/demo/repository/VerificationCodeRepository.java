package com.attendance.demo.repository;

import com.attendance.demo.entity.VerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VerificationCodeRepository extends JpaRepository<VerificationCode, Long> {

    Optional<VerificationCode> findByUserIdAndPurpose(Long userId, VerificationCode.Purpose purpose);

    void deleteByUserIdAndPurpose(Long userId, VerificationCode.Purpose purpose);
}
