package com.svatantra.spotme.spotme.controller;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import com.svatantra.spotme.spotme.dto.claimList.ClaimListResponse;
import com.svatantra.spotme.spotme.service.SpotMeService;
import com.svatantra.spotme.spotme.dto.activities.ActivityResponse;
import com.svatantra.spotme.spotme.dto.claimActions.ClaimActionRequest;
import com.svatantra.spotme.spotme.dto.claimActions.ClaimActionResponse;
import com.svatantra.spotme.spotme.dto.activities.ActivityRequest;
import jakarta.validation.Valid;
@Validated
@RestController
@RequestMapping("/api/spotme")
public class SpotMeController {
    private final SpotMeService spotMeService;

    public SpotMeController(
            SpotMeService spotMeService
    ) {
        this.spotMeService = spotMeService;
    }

    @GetMapping("/catalog")
    public List<String> getCatalog(
            @NotBlank
            @RequestParam String type
    ) {

        return spotMeService.getCatalog(type);
    }

    @GetMapping("/claimList")
    public List<ClaimListResponse> getClaimList(
            @RequestParam(required = false) String scope,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String date
    )
{
        return spotMeService.getClaimList(
                scope,
                month,
                status,
                sort,
                userId,
                date
        );
}
    @GetMapping("/claimList/{claimId}")
    public Object getClaimDetail(
            @PathVariable String claimId,
            @RequestParam(required = false)
            String view
    ) {
        return spotMeService.getClaimDetail(
                claimId,
                view
        );
    }
    @PostMapping("/claimList/activities")
    public ActivityResponse activityAction(
            @Valid @RequestBody ActivityRequest request
    )
    {
        return spotMeService.activityAction(
                request
        );
    }
    @PostMapping("/claimList/{claimId}/action")
    public ClaimActionResponse claimAction(
            @PathVariable String claimId,
            @Valid @RequestBody ClaimActionRequest request
    ) {
        return spotMeService.claimAction(
                claimId,
                request
        );
    }
}
