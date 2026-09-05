package com.svatantra.spotme.spotme.dto.claimList;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.time.LocalDateTime;
public record ClaimListResponse(
        UUID claimId,
        UUID userId,
        UUID branchId,
        LocalDate travelDate,
        String status,
        BigDecimal taggedTotalKm,
        BigDecimal totalKm,
        BigDecimal totalAmount,
        Integer activityCount,
        BigDecimal ratePerKm,
        LocalDateTime submittedAt,
        LocalDateTime claimedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}