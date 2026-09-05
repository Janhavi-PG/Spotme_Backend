package com.svatantra.spotme.spotme.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
@Entity
@Table(name = "travel_claims")
@Getter
@Setter
public class TravelClaim {

    @Id
    @Column(name = "travel_id")
    private UUID travelId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "branch_id")
    private UUID branchId;

    @Column(name = "travel_date")
    private LocalDate travelDate;

    @Column(name = "status")
    private String status;

    @Column(name = "activities_json", columnDefinition = "TEXT")
    private String activitiesJson;

    @Column(name = "tagged_total_km")
    private BigDecimal taggedTotalKm;

    @Column(name = "total_km")
    private BigDecimal totalKm;


    @Column(name = "total_amount")
    private BigDecimal totalAmount;

    @Column(name = "activity_count")
    private Integer activityCount;

    @Column(name = "rate_per_km")
    private BigDecimal ratePerKm;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "claimed_at")
    private LocalDateTime claimedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}