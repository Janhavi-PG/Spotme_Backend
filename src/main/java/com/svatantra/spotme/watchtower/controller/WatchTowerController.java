package com.svatantra.spotme.watchtower.controller;

import com.svatantra.spotme.spotme.entity.TravelClaim;
import com.svatantra.spotme.watchtower.service.WatchTowerService;
import org.springframework.web.bind.annotation.*;
import com.svatantra.spotme.watchtower.dto.RouteResponse;
import java.util.List;
import java.util.List;
@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/watchtower")
public class WatchTowerController {

    private final WatchTowerService watchTowerService;

    public WatchTowerController(
            WatchTowerService watchTowerService
    ) {
        this.watchTowerService = watchTowerService;
    }

    @GetMapping("/claims")
    public Object getClaims(
            @RequestParam Integer page,
            @RequestParam Integer size
    ) {
        return watchTowerService.getClaims(
                page,
                size
        );
    }

    @GetMapping("/claims/{travelId}")
    public TravelClaim getClaim(
            @PathVariable String travelId
    ) {
        return watchTowerService.getClaim(
                travelId
        );
    }
    @GetMapping("/routes")
    public List<RouteResponse> getRoutes()
            throws Exception {

        return watchTowerService.getRoutes();
    }
}
