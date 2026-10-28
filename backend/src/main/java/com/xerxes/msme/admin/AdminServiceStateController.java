package com.xerxes.msme.admin;

import com.xerxes.msme.admin.dto.AdminServiceStateResponse;
import com.xerxes.msme.admin.dto.UpdateServiceStateStatusRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Platform-staff-only: enable/disable GST states. See SecurityConfig for the role gate. */
@RestController
@RequestMapping("/api/admin/service-states")
public class AdminServiceStateController {

    private final AdminServiceStateService serviceStateService;

    public AdminServiceStateController(AdminServiceStateService serviceStateService) {
        this.serviceStateService = serviceStateService;
    }

    @GetMapping
    public List<AdminServiceStateResponse> list() {
        return serviceStateService.listAll();
    }

    @PutMapping("/{code}/status")
    public AdminServiceStateResponse updateStatus(
            @PathVariable String code, @Valid @RequestBody UpdateServiceStateStatusRequest request) {
        return serviceStateService.updateStatus(code, request.active());
    }
}
