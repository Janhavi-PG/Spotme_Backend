package com.svatantra.spotme.watchtower.dto;

import java.util.List;

public record RouteResponse(
        String date,
        List<RoutePointResponse> routePoints,
        List<DestinationPointResponse> destinationPoints
) {
}