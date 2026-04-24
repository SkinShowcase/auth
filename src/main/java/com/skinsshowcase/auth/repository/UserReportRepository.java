package com.skinsshowcase.auth.repository;

import com.skinsshowcase.auth.entity.UserReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.UUID;

public interface UserReportRepository extends JpaRepository<UserReport, UUID> {

    boolean existsByReporterSteamIdAndReportedSteamIdAndCreatedAtAfter(String reporterSteamId,
                                                                         String reportedSteamId,
                                                                         Instant after);

    Page<UserReport> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
