package com.xerxes.msme.admin;

import com.xerxes.msme.admin.dto.AdminServiceAreaResponse;
import com.xerxes.msme.admin.dto.CreateServiceAreaRequest;
import com.xerxes.msme.admin.dto.UpdateServiceAreaRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Platform-staff-only: manage which areas (with their pincode) the marketplace supports under a city. See SecurityConfig for the role gate. */
@RestController
@RequestMapping("/api/admin/service-areas")
public class AdminServiceAreaController {

    private final AdminServiceLocationService locationService;

    public AdminServiceAreaController(AdminServiceLocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping
    public List<AdminServiceAreaResponse> list(@RequestParam UUID cityId) {
        return locationService.listAreas(cityId);
    }

    @PostMapping
    public ResponseEntity<AdminServiceAreaResponse> create(@Valid @RequestBody CreateServiceAreaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(locationService.createArea(request));
    }

    @PutMapping("/{id}")
    public AdminServiceAreaResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateServiceAreaRequest request) {
        return locationService.updateArea(id, request);
    }
}
