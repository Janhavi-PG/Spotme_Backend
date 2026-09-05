package com.svatantra.spotme.spotme.dto.activities;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;


public record ActivityRequest(

        @NotBlank
        String action,

        String travelId,

        @Valid
        @NotNull
        ActivityItem activity

) {
}