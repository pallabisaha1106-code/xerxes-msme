package com.xerxes.msme.admin;

import com.xerxes.msme.admin.dto.AdminMarketplaceCategoryResponse;
import com.xerxes.msme.admin.dto.UpdateMarketplaceCategoryBookableRequest;
import com.xerxes.msme.admin.dto.UpdateMarketplaceCategoryImageProfileRequest;
import com.xerxes.msme.admin.dto.UpdateMarketplaceCategoryStatusRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Platform-staff-only: enable/disable marketplace categories, set how their photos are handled and whether shops in them take appointments. See SecurityConfig for the role gate. */
@RestController
@RequestMapping("/api/admin/marketplace-categories")
public class AdminMarketplaceCategoryController {

    private final AdminMarketplaceCategoryService categoryService;

    public AdminMarketplaceCategoryController(AdminMarketplaceCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public List<AdminMarketplaceCategoryResponse> list() {
        return categoryService.listAll();
    }

    @PutMapping("/{id}/status")
    public AdminMarketplaceCategoryResponse updateStatus(
            @PathVariable UUID id, @Valid @RequestBody UpdateMarketplaceCategoryStatusRequest request) {
        return categoryService.updateStatus(id, request.active());
    }

    @PutMapping("/{id}/image-profile")
    public AdminMarketplaceCategoryResponse updateImageProfile(
            @PathVariable UUID id, @Valid @RequestBody UpdateMarketplaceCategoryImageProfileRequest request) {
        return categoryService.updateImageProfile(id, request.imageProfile());
    }

    @PutMapping("/{id}/bookable")
    public AdminMarketplaceCategoryResponse updateBookable(
            @PathVariable UUID id, @Valid @RequestBody UpdateMarketplaceCategoryBookableRequest request) {
        return categoryService.updateBookable(id, request.bookable());
    }
}
