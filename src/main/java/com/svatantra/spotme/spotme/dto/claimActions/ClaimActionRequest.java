package com.svatantra.spotme.spotme.dto.claimActions;
import com.svatantra.spotme.spotme.dto.activities.ActivityItem;
import jakarta.validation.constraints.NotBlank;
public record ClaimActionRequest(

        @NotBlank
        String action,

        String dayId,

        String rejectionReasonCode,

        String comment,

        ActivityItem activity

) {
}