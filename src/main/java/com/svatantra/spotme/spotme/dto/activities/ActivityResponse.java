package com.svatantra.spotme.spotme.dto.activities;

public record ActivityResponse(

        String message,

        String travelId,

        String purposeName,

        String centre,

        Double roadDistanceKm,

        Double claimAmount,

        String loggedAt

) {
}