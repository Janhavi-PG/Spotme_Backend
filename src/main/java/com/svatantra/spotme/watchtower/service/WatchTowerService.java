package com.svatantra.spotme.watchtower.service;

import com.svatantra.spotme.spotme.entity.TravelClaim;
import com.svatantra.spotme.spotme.repository.TravelClaimRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Sort;
import com.svatantra.common.exception.ResourceNotFoundException;
import com.svatantra.common.logging.StructuredLogger;
import java.util.UUID;
import com.svatantra.spotme.spotme.dto.activities.ActivityItem;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;
import java.util.List;
import com.svatantra.spotme.spotme.service.GraphHopperService;
import com.svatantra.spotme.watchtower.dto.RouteResponse;
import java.util.ArrayList;
import com.svatantra.spotme.watchtower.dto.RoutePointResponse;
import com.svatantra.spotme.watchtower.dto.DestinationPointResponse;
@Service
public class WatchTowerService {

    private final TravelClaimRepository travelClaimRepository;
    private final JsonMapper objectMapper;
    private static final StructuredLogger log =
            StructuredLogger.forClass(WatchTowerService.class);
    private final GraphHopperService graphHopperService;

    public WatchTowerService(
            TravelClaimRepository travelClaimRepository,
            JsonMapper objectMapper,
            GraphHopperService graphHopperService
    ) {
        this.travelClaimRepository = travelClaimRepository;
        this.objectMapper = objectMapper;
        this.graphHopperService = graphHopperService;
    }

    public Page<TravelClaim> getClaims(
            Integer page,
            Integer size
    ) {
        Pageable pageable =
                PageRequest.of(
                        page,
                        size,
                        Sort.by("travelDate").descending()
                );

        return travelClaimRepository.findAll(pageable);
    }

    public TravelClaim getClaim(
            String travelId
    ) {
        UUID uuid = UUID.fromString(travelId);

        return travelClaimRepository
                .findByTravelId(uuid)
                .orElseThrow(() -> {

                    log.event("TRAVEL_CLAIM_NOT_FOUND")
                            .field("travelId", travelId)
                            .warn();

                    return new ResourceNotFoundException(
                            "Travel claim not found"
                    );
                });
    }

    private List<ActivityItem> getActivities(
            TravelClaim claim
    ) throws Exception {

        String activitiesJson =
                claim.getActivitiesJson();

        if (activitiesJson == null
                || activitiesJson.isBlank()) {

            return List.of();
        }

        return objectMapper.readValue(
                activitiesJson,
                new TypeReference<List<ActivityItem>>() {
                }
        );
    }

    public void testActivities(
            String travelId
    ) throws Exception {

        TravelClaim claim =
                getClaim(travelId);

        List<ActivityItem> activities =
                getActivities(claim);

        System.out.println(
                "ACTIVITY COUNT = "
                        + activities.size()
        );

        activities.forEach(activity ->
                System.out.println(
                        activity.originLat()
                                + " -> "
                                + activity.destinationLat()
                )
        );
    }
    public List<RouteResponse> getRoutes()
            throws Exception {

        Pageable pageable =
                PageRequest.of(
                        0,
                        1000,
                        Sort.by("travelDate").descending()
                );

        List<TravelClaim> claims =
                travelClaimRepository
                        .findAll(pageable)
                        .getContent();
        List<RouteResponse> routes =
                new ArrayList<>();

        int processedClaims = 0;
        for (TravelClaim claim : claims) {
            if (processedClaims >= 5) {
                break;
            }


            List<ActivityItem> activities =
                    getActivities(claim);


            if (activities.isEmpty()) {
                continue;
            }
            List<RoutePointResponse> routePoints =
                    graphHopperService.getRoutePointsForDay(
                            activities
                    );
            List<DestinationPointResponse> destinationPoints =
                    activities.stream()
                            .filter(activity ->
                                    activity.destinationLat() != null
                                            && activity.destinationLng() != null
                            )
                            .map(activity ->
                                    new DestinationPointResponse(
                                            activity.destinationLat(),
                                            activity.destinationLng()
                                    )
                            )
                            .toList();
            routes.add(
                    new RouteResponse(
                            claim.getTravelDate().toString(),
                            routePoints,
                            destinationPoints
                    )
            );

            processedClaims++;
        }
        return routes;
    }
}