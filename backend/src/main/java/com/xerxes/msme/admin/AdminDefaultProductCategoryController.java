package com.xerxes.msme.admin;

import com.xerxes.msme.admin.dto.AdminDefaultProductCategoryResponse;
import com.xerxes.msme.admin.dto.CreateDefaultProductCategoryRequest;
import com.xerxes.msme.admin.dto.UpdateDefaultProductCategoryRequest;
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

/** Platform-staff-only: manage the starter catalog's sub-categories. See SecurityConfig for the role gate. */
@RestController
@RequestMapping("/api/admin/default-product-categories")
public class AdminDefaultProductCategoryController {

    private final AdminDefaultProductCatalogService catalogService;

    public AdminDefaultProductCategoryController(AdminDefaultProductCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public List<AdminDefaultProductCategoryResponse> list(@RequestParam UUID marketplaceCategoryId) {
        return catalogService.listCategories(marketplaceCategoryId);
    }

    @PostMapping
    public ResponseEntity<AdminDefaultProductCategoryResponse> create(
            @Valid @RequestBody CreateDefaultProductCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogService.createCategory(request));
    }

    @PutMapping("/{id}")
    public AdminDefaultProductCategoryResponse update(
            @PathVariable UUID id, @Valid @RequestBody UpdateDefaultProductCategoryRequest request) {
        return catalogService.updateCategory(id, request);
    }
}
