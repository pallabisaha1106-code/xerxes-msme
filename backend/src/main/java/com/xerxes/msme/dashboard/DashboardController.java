package com.xerxes.msme.dashboard;

import com.xerxes.msme.dashboard.dto.DashboardResponse;
import com.xerxes.msme.security.AuthContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final AuthContext authContext;

    public DashboardController(DashboardService dashboardService, AuthContext authContext) {
        this.dashboardService = dashboardService;
        this.authContext = authContext;
    }

    @GetMapping
    public DashboardResponse get() {
        return dashboardService.getDashboard(authContext.current().tenantId());
    }
}
