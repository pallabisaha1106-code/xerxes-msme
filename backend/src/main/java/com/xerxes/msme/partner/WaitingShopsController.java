package com.xerxes.msme.partner;

import com.xerxes.msme.partner.dto.AssignWaitingResponse;
import com.xerxes.msme.partner.dto.WaitingAreaResponse;
import com.xerxes.msme.security.AuthContext;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Platform-staff-only: the areas with shops that need a partner, and the button that hands them out. */
@RestController
@RequestMapping("/api/admin/partner-waiting-areas")
public class WaitingShopsController {

    private final WaitingShopsService waitingShopsService;
    private final AuthContext authContext;

    public WaitingShopsController(WaitingShopsService waitingShopsService, AuthContext authContext) {
        this.waitingShopsService = waitingShopsService;
        this.authContext = authContext;
    }

    @GetMapping
    public List<WaitingAreaResponse> list() {
        return waitingShopsService.list();
    }

    @PostMapping("/{areaId}/assign")
    public AssignWaitingResponse assign(@PathVariable UUID areaId) {
        return waitingShopsService.assign(areaId, authContext.current().userId());
    }
}
