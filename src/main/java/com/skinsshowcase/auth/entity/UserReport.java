package com.skinsshowcase.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_report")
public class UserReport {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "reporter_steam_id", nullable = false, length = 64)
    private String reporterSteamId;

    @Column(name = "reported_steam_id", nullable = false, length = 64)
    private String reportedSteamId;

    @Column(name = "reason", nullable = false, length = 64)
    private String reason;

    @Column(name = "details", length = 2000)
    private String details;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected UserReport() {
    }

    public UserReport(UUID id, String reporterSteamId, String reportedSteamId, String reason, String details,
                      Instant createdAt) {
        this.id = id;
        this.reporterSteamId = reporterSteamId;
        this.reportedSteamId = reportedSteamId;
        this.reason = reason;
        this.details = details;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getReporterSteamId() {
        return reporterSteamId;
    }

    public String getReportedSteamId() {
        return reportedSteamId;
    }

    public String getReason() {
        return reason;
    }

    public String getDetails() {
        return details;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
