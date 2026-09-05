package com.svatantra.spotme.spotme.dto.claimDetail;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.time.LocalDateTime;
public record ClaimSummaryResponse(
        UUID claimId,
        LocalDate travelDate,
        String status,
        BigDecimal taggedTotalKm,
        BigDecimal totalKm,
        BigDecimal ratePerKm,
        BigDecimal totalAmount,
        Integer activityCount,
        LocalDateTime updatedAt
) {
}