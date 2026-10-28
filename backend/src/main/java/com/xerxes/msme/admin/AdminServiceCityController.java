package com.xerxes.msme.admin;

import com.xerxes.msme.admin.dto.AdminServiceCityResponse;
import com.xerxes.msme.admin.dto.CreateServiceCityRequest;
import com.xerxes.msme.admin.dto.UpdateServiceCityRequest;
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

/** Platform-staff-only: manage which cities the marketplace supports. See SecurityConfig for the role gate. */
@RestController
@RequestMapping("/api/admin/service-cities")
public class AdminServiceCityController {

    private final AdminServiceLocationService locationService;

    public AdminServiceCityController(AdminServiceLocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping
    public List<AdminServiceCityResponse> list(@RequestParam String stateCode) {
        return locationService.listCities(stateCode);
    }

    @PostMapping
    public ResponseEntity<AdminServiceCityResponse> create(@Valid @RequestBody CreateServiceCityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(locationService.createCity(request));
    }

    @PutMapping("/{id}")
    public AdminServiceCityResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateServiceCityRequest request) {
        return locationService.updateCity(id, request);
    }
}
