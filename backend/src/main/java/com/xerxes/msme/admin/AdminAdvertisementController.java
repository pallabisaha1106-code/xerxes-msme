package com.xerxes.msme.admin;

import com.xerxes.msme.admin.dto.AdminAdvertisementResponse;
import com.xerxes.msme.admin.dto.CreateAdvertisementRequest;
import com.xerxes.msme.admin.dto.UpdateAdvertisementRequest;
import com.xerxes.msme.marketplace.AdvertisementSlot;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Platform-staff-only: manage the marketplace's 3 ad-slot advertisements. See SecurityConfig for the role gate. */
@RestController
@RequestMapping("/api/admin/advertisements")
public class AdminAdvertisementController {

    private final AdminAdvertisementService advertisementService;

    public AdminAdvertisementController(AdminAdvertisementService advertisementService) {
        this.advertisementService = advertisementService;
    }

    @GetMapping
    public List<AdminAdvertisementResponse> list(@RequestParam AdvertisementSlot slot) {
        return advertisementService.listBySlot(slot);
    }

    @PostMapping
    public ResponseEntity<AdminAdvertisementResponse> create(@Valid @RequestBody CreateAdvertisementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(advertisementService.create(request));
    }

    @PutMapping("/{id}")
    public AdminAdvertisementResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateAdvertisementRequest request) {
        return advertisementService.update(id, request);
    }

    @PostMapping(path = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AdminAdvertisementResponse uploadPhoto(@PathVariable UUID id, @RequestPart("file") MultipartFile file) {
        return advertisementService.uploadPhoto(id, file);
    }

    @PutMapping("/{id}/publish")
    public AdminAdvertisementResponse publish(@PathVariable UUID id) {
        return advertisementService.setPublished(id, true);
    }

    @PutMapping("/{id}/unpublish")
    public AdminAdvertisementResponse unpublish(@PathVariable UUID id) {
        return advertisementService.setPublished(id, false);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        advertisementService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
