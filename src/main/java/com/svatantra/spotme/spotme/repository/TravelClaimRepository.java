package com.svatantra.spotme.spotme.repository;

import com.svatantra.spotme.spotme.entity.TravelClaim;
import org.springframework.data.jpa.repository.JpaRepository;


import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface TravelClaimRepository
        extends JpaRepository<TravelClaim, UUID> {

    Optional<TravelClaim> findByTravelId(
            UUID travelId
    );

    Optional<TravelClaim> findByUserIdAndTravelDate(
            UUID userId,
            LocalDate travelDate
    );

    Optional<TravelClaim> findByTravelDate(
            LocalDate travelDate
    );
}