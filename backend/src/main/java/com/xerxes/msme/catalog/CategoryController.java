package com.xerxes.msme.catalog;

import com.xerxes.msme.catalog.dto.CategoryRequest;
import com.xerxes.msme.catalog.dto.CategoryResponse;
import com.xerxes.msme.common.BulkImportResult;
import com.xerxes.msme.common.BulkImportSupport;
import com.xerxes.msme.security.AuthContext;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;
    private final AuthContext authContext;
    private final BulkImportSupport bulkImportSupport;

    public CategoryController(CategoryService categoryService, AuthContext authContext, BulkImportSupport bulkImportSupport) {
        this.categoryService = categoryService;
        this.authContext = authContext;
        this.bulkImportSupport = bulkImportSupport;
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryRequest request) {
        CategoryResponse response = categoryService.create(authContext.current().tenantId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/bulk")
    public BulkImportResult bulkCreate(@RequestBody List<CategoryRequest> requests) {
        UUID tenantId = authContext.current().tenantId();
        return bulkImportSupport.run(requests, request -> categoryService.create(tenantId, request));
    }

    @GetMapping
    public List<CategoryResponse> list() {
        return categoryService.list(authContext.current().tenantId());
    }

    @GetMapping("/{id}")
    public CategoryResponse get(@PathVariable UUID id) {
        return categoryService.get(authContext.current().tenantId(), id);
    }
}
