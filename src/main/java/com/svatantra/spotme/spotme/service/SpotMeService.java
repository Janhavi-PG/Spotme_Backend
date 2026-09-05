package com.svatantra.spotme.spotme.service;
import com.svatantra.common.logging.StructuredLogger;
import com.svatantra.spotme.spotme.dto.claimDetail.ClaimDetailResponse;
import com.svatantra.spotme.spotme.dto.claimDetail.ClaimSummaryResponse;
import com.svatantra.spotme.spotme.dto.claimList.ClaimListResponse;
import com.svatantra.spotme.spotme.entity.TravelClaim;
import com.svatantra.spotme.spotme.enums.PurposeType;
import com.svatantra.spotme.spotme.repository.TravelClaimRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.svatantra.spotme.spotme.dto.activities.ActivityItem;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import com.svatantra.spotme.spotme.dto.activities.ActivityResponse;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;
import com.svatantra.spotme.spotme.dto.claimActions.ClaimActionRequest;
import com.svatantra.spotme.spotme.dto.claimActions.ClaimActionResponse;
import com.svatantra.spotme.spotme.dto.activities.ActivityRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import com.svatantra.common.exception.ResourceNotFoundException;
import com.svatantra.common.exception.ValidationException;
import com.svatantra.common.exception.ExternalServiceException;
@Service
@RequiredArgsConstructor
public class SpotMeService {

    private final TravelClaimRepository travelClaimRepository;
    private final JsonMapper objectMapper;
    private final GraphHopperService graphHopperService;
    private static final StructuredLogger log =
            StructuredLogger.forClass(SpotMeService.class);

    public List<String> getCatalog(String type) {
        return Arrays.stream(PurposeType.values())
                .map(Enum::name)
                .toList();
    }


    public List<ClaimListResponse> getClaimList(

            String scope, String month, String status, String sort, String userId, String date
    ) {

        List<TravelClaim> claims = travelClaimRepository.findAll();

        if (scope != null && !scope.isBlank()) {
            UUID branchId = UUID.fromString(scope);
            claims = claims.stream()
                    .filter(claim -> branchId.equals(claim.getBranchId()))
                    .toList();
        }

        if (userId != null && !userId.isBlank()) {
            UUID userUuid = UUID.fromString(userId);
            claims = claims.stream()
                    .filter(claim -> userUuid.equals(claim.getUserId()))
                    .toList();
        }

        if (status != null && !status.isBlank()) {
            claims = claims.stream()
                    .filter(claim -> status.equalsIgnoreCase(claim.getStatus()))
                    .toList();
        }

        if (date != null && !date.isBlank()) {
            LocalDate selectedDate = LocalDate.parse(date);
            claims = claims.stream()
                    .filter(claim -> selectedDate.equals(claim.getTravelDate()))
                    .toList();
        }

        if (month != null && !month.isBlank()) {
            YearMonth yearMonth = YearMonth.parse(month);
            LocalDate startDate = yearMonth.atDay(1);
            LocalDate endDate = yearMonth.atEndOfMonth();
            claims = claims.stream().filter(claim -> !claim.getTravelDate().isBefore(startDate) && !claim.getTravelDate().isAfter(endDate)).toList();
        }

        if ("latest".equalsIgnoreCase(sort)) {
            claims = claims.stream()
                    .sorted(Comparator.comparing(TravelClaim::getTravelDate).reversed())
                    .toList();
        }
        log.event("CLAIM_LIST_FETCHED")
                .field("scope", scope)
                .field("status", status)
                .field("month", month)
                .field("resultCount", claims.size())
                .info();
        return mapToClaimListResponseList(claims);
    }

    public Object getClaimDetail(
            String claimId, String view
    ) {

        UUID claimUuid = UUID.fromString(claimId);
        TravelClaim claim = travelClaimRepository
                .findByTravelId(claimUuid)
                .orElseThrow(() -> {

                    log.event("TRAVEL_CLAIM_NOT_FOUND")
                            .field("claimId", claimId)
                            .warn();

                    return new ResourceNotFoundException(
                            "Travel claim not found"
                    );
                });
        log.event("CLAIM_DETAIL_FETCHED")
                .field("claimId", claimId)
                .field("view", view)
                .info();
        if ("summary".equalsIgnoreCase(view)) {
            return mapToClaimSummaryResponse(claim);
        }

        if ("detail".equalsIgnoreCase(view)) {
            return mapToClaimDetailResponse(claim);
        }
        return mapToClaimSummaryResponse(claim);
    }

    public ActivityResponse activityAction(
            ActivityRequest request
    )
    {
        TravelClaim claim;

        if (request.travelId() == null
                || request.travelId().isBlank()) {

            claim = travelClaimRepository
                    .findByTravelDate(
                            LocalDate.now()
                    )
                    .orElseGet(
                            this::createTravelClaim
                    );

        } else {

            claim = travelClaimRepository
                    .findByTravelId(
                            UUID.fromString(
                                    request.travelId()
                            )
                    )
                    .orElseThrow(() -> {

                        log.event("TRAVEL_CLAIM_NOT_FOUND")
                                .field("travelId", request.travelId())
                                .warn();

                        return new ResourceNotFoundException(
                                "Travel claim not found"
                        );
                    });
        }

        if ("add_activity".equalsIgnoreCase(
                request.action()
        )) {
            try {
                log.event("ACTIVITY_DISTANCE_CALCULATION_STARTED")
                        .field("originLat", request.activity().originLat())
                        .field("originLng", request.activity().originLng())
                        .field("destinationLat", request.activity().destinationLat())
                        .field("destinationLng", request.activity().destinationLng())
                        .info();
                List<ActivityItem> activities = getActivities(claim);
                ActivityItem newActivity;

                if (request.travelId() == null
                        || request.travelId().isBlank()) {

                    newActivity = createActivity(
                            request,
                            claim,
                            activities.size() + 1
                    );

                } else {double roadDistanceKm =
                        graphHopperService.calculateDistance(
                                request.activity().originLat(),
                                request.activity().originLng(),
                                request.activity().destinationLat(),
                                request.activity().destinationLng()
                        ) / 1000;


                    newActivity = new ActivityItem(
                            UUID.randomUUID().toString(),
                            activities.size() + 1,
                            request.activity().purposeName(),
                            request.activity().originLat(),
                            request.activity().originLng(),
                            request.activity().destinationLat(),
                            request.activity().destinationLng(),
                            roadDistanceKm,
                            request.activity().attachments(),
                            roadDistanceKm
                                    * claim.getRatePerKm()
                                    .doubleValue(),                            false,
                            request.activity().overrideDistanceKm(),
                            request.activity().overrideReason(),
                            request.activity().sfoComment(),
                            "manual",
                            java.time.LocalDateTime.now().toString()
                    );
                }
                activities.add(newActivity);

                BigDecimal taggedTotalKm =
                        activities.stream()
                                .map(a ->
                                        BigDecimal.valueOf(
                                                a.roadDistanceKm()
                                        ))
                                .reduce(
                                        BigDecimal.ZERO,
                                        BigDecimal::add
                                );

                claim.setTaggedTotalKm(
                        taggedTotalKm
                );
                claim.setTotalKm(
                        taggedTotalKm
                );
                claim.setActivityCount(
                        activities.size()
                );

                claim.setTotalAmount(
                        taggedTotalKm.multiply(
                                claim.getRatePerKm()
                        )
                );

                String updatedActivitiesJson =
                        objectMapper.writeValueAsString(
                                activities
                        );

                claim.setActivitiesJson(
                        updatedActivitiesJson
                );



                
                claim.setUpdatedAt(
                        java.time.LocalDateTime.now()
                );

                travelClaimRepository.save(claim);
                log.event("ACTIVITY_ADDED")
                        .field("travelId", claim.getTravelId())
                        .field("activityId", newActivity.activityId())
                        .field("purpose", newActivity.purposeName())
                        .field("distanceKm", newActivity.roadDistanceKm())
                        .info();
                return new ActivityResponse(
                        "Activity Added Successfully",
                        claim.getTravelId().toString(),
                        newActivity.purposeName(),
                        "Selected Centre",
                        newActivity.roadDistanceKm(),
                        newActivity.claimAmount(),
                        newActivity.loggedAt()
                );

            } catch (ExternalServiceException exception) {
        throw exception;

    } catch (Exception exception) {
        log.event("ACTIVITY_ADD_FAILED")
                .field("action", request.action())
                .error(exception);

        return new ActivityResponse(
                exception.getMessage(),
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
        }

        if ("update_activity".equalsIgnoreCase(
                request.action()
        )) {
            try {
                List<ActivityItem> activities = getActivities(claim);

                for (int i = 0; i < activities.size(); i++) {
                    ActivityItem existing = activities.get(i);

                    if (existing.activityId().equals(
                            request.activity().activityId()
                    )) {
                        ActivityItem updated = new ActivityItem(
                                existing.activityId(),
                                existing.sequence(),
                                request.activity().purposeName(),
                                existing.originLat(),
                                existing.originLng(),
                                existing.destinationLat(),
                                existing.destinationLng(),

                                existing.roadDistanceKm(),
                                request.activity().attachments(),

                                request.activity().overrideDistanceKm()
                                        * claim.getRatePerKm().doubleValue(),

                                true,

                                request.activity().overrideDistanceKm(),
                                request.activity().overrideReason(),
                                request.activity().sfoComment(),
                                existing.routingProvider(),
                                existing.loggedAt()
                        );
                        activities.set(i, updated);

                        double totalKm = activities.stream()
                                .mapToDouble(activity ->
                                        activity.isManualOverride()
                                                && activity.overrideDistanceKm() != null
                                                ? activity.overrideDistanceKm()
                                                : activity.roadDistanceKm()
                                )
                                .sum();

                        claim.setTotalKm(
                                BigDecimal.valueOf(totalKm)
                        );

                        BigDecimal taggedTotalKm =
                                activities.stream()
                                        .map(a ->
                                                BigDecimal.valueOf(
                                                        a.roadDistanceKm()
                                                ))
                                        .reduce(
                                                BigDecimal.ZERO,
                                                BigDecimal::add
                                        );

                        claim.setTaggedTotalKm(
                                taggedTotalKm
                        );

                        claim.setTotalAmount(
                                claim.getTotalKm()
                                        .multiply(
                                                claim.getRatePerKm()
                                        )
                        );

                        claim.setActivityCount(
                                activities.size()
                        );

                        String updatedActivitiesJson =
                                objectMapper.writeValueAsString(
                                        activities
                                );

                        claim.setActivitiesJson(
                                updatedActivitiesJson
                        );

                        claim.setUpdatedAt(
                                java.time.LocalDateTime.now()
                        );

                        travelClaimRepository.save(claim);
                        log.event("ACTIVITY_UPDATED")
                                .field("travelId", claim.getTravelId())
                                .field("activityId", existing.activityId())
                                .info();

                        return new ActivityResponse(
                                "Activity Updated Successfully",
                                null,
                                null,
                                null,
                                null,
                                null,
                                null
                        );

                    }
                }
                log.event("ACTIVITY_NOT_FOUND")
                        .field("activityId", request.activity().activityId())
                        .warn();

                throw new ResourceNotFoundException(
                        "Activity not found"
                );


            } catch (ResourceNotFoundException exception) {
                throw exception;
            }
            catch (ExternalServiceException exception) {
                throw exception;
            }
            catch (Exception exception) {
                log.event("ACTIVITY_UPDATE_FAILED")
                        .field("action", request.action())
                        .error(exception);

                return new ActivityResponse(
                        exception.getMessage(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                );
            }
        }

        return new ActivityResponse(
                "Unsupported Action",
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
    public ClaimActionResponse claimAction(
            String claimId, ClaimActionRequest request
    ) {

        UUID claimUuid = UUID.fromString(claimId);

        TravelClaim claim = travelClaimRepository
                .findByTravelId(claimUuid)
                .orElseThrow(() -> {

                    log.event("TRAVEL_CLAIM_NOT_FOUND")
                            .field("claimId", claimId)
                            .warn();

                    return new ResourceNotFoundException(
                            "Travel claim not found"
                    );
                });

        if ("submit".equalsIgnoreCase(
                request.action()
        )) {
            try {
                List<ActivityItem> activities = getActivities(claim);
                if (activities.isEmpty()) {

                    log.event("CLAIM_SUBMISSION_VALIDATION_FAILED")
                            .field("travelId", claim.getTravelId())
                            .warn();

                    throw new ValidationException(
                            "Cannot submit claim without activities"
                    );
                }
                double totalKm = activities.stream()
                        .mapToDouble(activity ->
                                activity.overrideDistanceKm() != null
                                        ? activity.overrideDistanceKm()
                                        : activity.roadDistanceKm()
                        )
                        .sum();

                claim.setTotalKm(
                        BigDecimal.valueOf(totalKm)
                );

                double taggedTotalKm = activities.stream()
                        .mapToDouble(ActivityItem::roadDistanceKm)
                        .sum();

                claim.setTaggedTotalKm(
                        BigDecimal.valueOf(taggedTotalKm)
                );

                claim.setActivityCount(
                        activities.size()
                );
                double totalAmount = activities.stream()
                        .mapToDouble(activity -> {

                            double effectiveKm =
                                    activity.overrideDistanceKm() != null
                                            ? activity.overrideDistanceKm()
                                            : activity.roadDistanceKm();

                            return effectiveKm
                                    * claim.getRatePerKm()
                                    .doubleValue();
                        })
                        .sum();

                claim.setTotalAmount(
                        BigDecimal.valueOf(totalAmount)
                );
                claim.setStatus("SUBMITTED");

                claim.setSubmittedAt(
                        java.time.LocalDateTime.now()
                );

                claim.setUpdatedAt(
                        java.time.LocalDateTime.now()
                );

                updateClaimAmount(claim);

                travelClaimRepository.save(claim);
                log.event("CLAIM_SUBMITTED")
                        .field("travelId", claim.getTravelId())
                        .field("totalKm", claim.getTotalKm())
                        .field("totalAmount", claim.getTotalAmount())
                        .info();
                return new ClaimActionResponse("Claim Submitted Successfully");

            } catch (ValidationException exception) {

                throw exception;

            }catch (Exception exception) {
                log.event("CLAIM_SUBMISSION_FAILED")
                        .field("travelId", claim.getTravelId())
                        .error(exception);
                return new ClaimActionResponse(exception.getMessage());
            }
        }

        if ("approve".equalsIgnoreCase(
                request.action()
        )) {
            if (!"SUBMITTED".equalsIgnoreCase(
                    claim.getStatus()
            )) {

                log.event("CLAIM_APPROVAL_VALIDATION_FAILED")
                        .field("travelId", claim.getTravelId())
                        .field("currentStatus", claim.getStatus())
                        .warn();

                throw new ValidationException(
                        "Only submitted claims can be approved"
                );
            }
            claim.setStatus("CLAIMED");
            claim.setClaimedAt(
                    java.time.LocalDateTime.now()
            );
            claim.setUpdatedAt(
                    java.time.LocalDateTime.now()
            );
            updateClaimAmount(claim);
            travelClaimRepository.save(claim);
            log.event("CLAIM_APPROVED")
                    .field("travelId", claim.getTravelId())
                    .info();
            return new ClaimActionResponse(
                    "Claim Approved Successfully"
            );
        }
        if ("reject".equalsIgnoreCase(request.action())) {
            if (!"SUBMITTED".equalsIgnoreCase(
                    claim.getStatus()
            )) {

                log.event("CLAIM_REJECTION_VALIDATION_FAILED")
                        .field("travelId", claim.getTravelId())
                        .field("currentStatus", claim.getStatus())
                        .warn();

                throw new ValidationException(
                        "Only submitted claims can be rejected"
                );
            }
            claim.setStatus("REJECTED");
            claim.setUpdatedAt(
                    java.time.LocalDateTime.now()
            );
            updateClaimAmount(claim);

            travelClaimRepository.save(claim);
            log.event("CLAIM_REJECTED")
                    .field("travelId", claim.getTravelId())
                    .info();
            return new ClaimActionResponse("Claim Rejected Successfully");

        }

        return new ClaimActionResponse("Unsupported Action");
    }
    private List<ActivityItem> getActivities(TravelClaim claim)
            throws Exception {
        String activitiesJson = claim.getActivitiesJson();

        if (activitiesJson == null || activitiesJson.isBlank()) {
            return List.of();
        }
        return objectMapper.readValue(activitiesJson, new TypeReference<List<ActivityItem>>() {
                }
        );
    }
    private void updateClaimAmount(
            TravelClaim claim
    ) {
        if (claim.getTotalKm() != null
                && claim.getRatePerKm() != null) {

            claim.setTotalAmount(
                    claim.getTotalKm()
                            .multiply(claim.getRatePerKm())
            );
        }
    }
    private ClaimListResponse mapToClaimListResponse(
            TravelClaim claim
    ) {
        int deviationPercentage = 0;

        if (claim.getTaggedTotalKm() != null
                && claim.getTaggedTotalKm().compareTo(BigDecimal.ZERO) > 0) {

            deviationPercentage =
                    Math.min(
                            100,
                            claim.getTotalKm()
                                    .subtract(claim.getTaggedTotalKm())
                                    .abs()
                                    .multiply(BigDecimal.valueOf(100))
                                    .divide(
                                            claim.getTaggedTotalKm(),
                                            0,
                                            RoundingMode.HALF_UP
                                    )
                                    .intValue()
                    );
        }
        BigDecimal totalAmount = BigDecimal.ZERO;

        if (claim.getTotalKm() != null
                && claim.getRatePerKm() != null) {

            totalAmount =
                    claim.getTotalKm()
                            .multiply(claim.getRatePerKm());
        }

        return new ClaimListResponse(
                claim.getTravelId(),
                claim.getUserId(),
                claim.getBranchId(),
                claim.getTravelDate(),
                claim.getStatus(),
                claim.getTaggedTotalKm(),
                claim.getTotalKm(),
                totalAmount,
                claim.getActivityCount(),
                claim.getRatePerKm(),
                claim.getSubmittedAt(),
                claim.getClaimedAt(),
                claim.getCreatedAt(),
                claim.getUpdatedAt()
        );
    }

    private List<ClaimListResponse> mapToClaimListResponseList(
            List<TravelClaim> claims
    ) {
        return claims.stream()
                .map(this::mapToClaimListResponse)
                .toList();
    }

    private ClaimSummaryResponse mapToClaimSummaryResponse(
            TravelClaim claim
    ) {
        BigDecimal totalAmount = BigDecimal.ZERO;

        if (claim.getTotalKm() != null
                && claim.getRatePerKm() != null) {

            totalAmount =
                    claim.getTotalKm()
                            .multiply(claim.getRatePerKm());
        }
        return new ClaimSummaryResponse(
                claim.getTravelId(),
                claim.getTravelDate(),
                claim.getStatus(),
                claim.getTaggedTotalKm(),
                claim.getTotalKm(),
                claim.getRatePerKm(),
                totalAmount,
                claim.getActivityCount(),
                claim.getUpdatedAt()
        );
    }

    private ClaimDetailResponse mapToClaimDetailResponse(
            TravelClaim claim
    ) {
        BigDecimal totalAmount = BigDecimal.ZERO;

        if (claim.getTotalKm() != null
                && claim.getRatePerKm() != null) {

            totalAmount =
                    claim.getTotalKm()
                            .multiply(claim.getRatePerKm());
        }
        List<ActivityItem> activities = List.of();
        try {
            activities = getActivities(claim);
        } catch (Exception exception) {
            activities = List.of();
        }
      
        return new ClaimDetailResponse(
                claim.getTravelId(), claim.getUserId(), claim.getBranchId(), claim.getTravelDate(), claim.getStatus(), claim.getTaggedTotalKm(), claim.getTotalKm(), claim.getRatePerKm(), totalAmount, claim.getActivityCount(), claim.getActivitiesJson()
        );
    }
    private double getRandomCoordinate(
            double min,
            double max
    ) {
        return min + (
                Math.random()
                        * (max - min)
        );
    }
    private TravelClaim createTravelClaim()
 {
        TravelClaim claim =
                new TravelClaim();

        claim.setTravelId(
                UUID.randomUUID()
        );

     claim.setBranchId(
             UUID.randomUUID()
     );


        claim.setUserId(
                UUID.randomUUID()
        );

        claim.setTravelDate(
                LocalDate.now()
        );

        claim.setStatus(
                "NOT_SUBMITTED"
        );

        claim.setTaggedTotalKm(
                BigDecimal.ZERO
        );

     claim.setTotalKm(
             BigDecimal.ZERO
     );

        claim.setRatePerKm(
                BigDecimal.valueOf(6)
        );

        claim.setActivitiesJson(
                "[]"
        );

        claim.setActivityCount(
                0
        );

        claim.setTotalAmount(
                BigDecimal.ZERO
        );

        claim.setSubmittedAt(
                null
        );

        claim.setClaimedAt(
                null
        );

        claim.setCreatedAt(
                java.time.LocalDateTime.now()
        );

     claim.setUpdatedAt(
             claim.getCreatedAt()
     );


        return claim;
    }
    private ActivityItem createActivity(


            ActivityRequest request,
            TravelClaim claim,
            int sequence
    ) throws Exception {

        List<ActivityItem> activities =
                getActivities(claim);

        double originLat;
        double originLng;

        if (sequence == 1) {

            originLat =
                    request.activity().originLat();

            originLng =
                    request.activity().originLng();

        } else {

            ActivityItem previousActivity =
                    activities.get(
                            activities.size() - 1
                    );

            originLat =
                    previousActivity.destinationLat();

            originLng =
                    previousActivity.destinationLng();
        }

        double destinationLat =
                request.activity().destinationLat();

        double destinationLng =
                request.activity().destinationLng();

        double roadDistanceKm =
                graphHopperService.calculateDistance(
                        originLat,
                        originLng,
                        destinationLat,
                        destinationLng
                ) / 1000;
        log.event("ACTIVITY_DISTANCE_CALCULATED")
                .field("originLat", originLat)
                .field("originLng", originLng)
                .field("destinationLat", destinationLat)
                .field("destinationLng", destinationLng)
                .field("roadDistanceKm", roadDistanceKm)
                .info();
        double claimAmount =
                roadDistanceKm
                        * claim.getRatePerKm()
                        .doubleValue();
        return new ActivityItem(
                UUID.randomUUID().toString(),
                sequence,
                request.activity().purposeName(),
                originLat,
                originLng,
                destinationLat,
                destinationLng,
                roadDistanceKm,
                request.activity().attachments(),
                claimAmount,
                false,
                null,
                null,
                request.activity().sfoComment(),
                "graphhopper",
                java.time.LocalDateTime.now().toString()
        );
    }
}

