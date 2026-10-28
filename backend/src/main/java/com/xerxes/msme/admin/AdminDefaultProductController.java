package com.xerxes.msme.admin;

import com.xerxes.msme.admin.dto.AdminDefaultProductResponse;
import com.xerxes.msme.admin.dto.CreateDefaultProductRequest;
import com.xerxes.msme.admin.dto.UpdateDefaultProductRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

/** Platform-staff-only: manage the starter catalog's default products. See SecurityConfig for the role gate. */
@RestController
@RequestMapping("/api/admin/default-products")
public class AdminDefaultProductController {

    private final AdminDefaultProductCatalogService catalogService;

    public AdminDefaultProductController(AdminDefaultProductCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public List<AdminDefaultProductResponse> list(@RequestParam UUID defaultProductCategoryId) {
        return catalogService.listProducts(defaultProductCategoryId);
    }

    @PostMapping
    public ResponseEntity<AdminDefaultProductResponse> create(@Valid @RequestBody CreateDefaultProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogService.createProduct(request));
    }

    @PutMapping("/{id}")
    public AdminDefaultProductResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateDefaultProductRequest request) {
        return catalogService.updateProduct(id, request);
    }

    @PostMapping(path = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AdminDefaultProductResponse uploadPhoto(@PathVariable UUID id, @RequestPart("file") MultipartFile file) {
        return catalogService.uploadProductPhoto(id, file);
    }
}
