package com.svatantra.spotme.spotme.dto.claimDetail;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ClaimDetailResponse(
        UUID claimId,
        UUID userId,
        UUID branchId,
        LocalDate travelDate,
        String status,
        BigDecimal taggedTotalKm,
        BigDecimal totalKm,
        BigDecimal ratePerKm,
        BigDecimal totalAmount,
        Integer activityCount,
        String activitiesJson
) {
}