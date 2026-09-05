package com.svatantra.spotme.spotme.dto.activities;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonAlias;
public record ActivityItem(
        @JsonProperty("activity_id")
        @JsonAlias("activityId")
        String activityId,

        Integer sequence,

        String purposeName,

        @JsonProperty("origin.lat")
        @JsonAlias("originLat")
        Double originLat,

        @JsonProperty("origin.lng")
        @JsonAlias("originLng")
        Double originLng,

        @JsonProperty("destination.lat")
        @JsonAlias("destinationLat")
        Double destinationLat,

        @JsonProperty("destination.lng")
        @JsonAlias("destinationLng")
        Double destinationLng,

        Double roadDistanceKm,

        List<String> attachments,

        Double claimAmount,

        Boolean isManualOverride,

        Double overrideDistanceKm,

        String overrideReason,

        String sfoComment,

        String routingProvider,

        String loggedAt
) {}