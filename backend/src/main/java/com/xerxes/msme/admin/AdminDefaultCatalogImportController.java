package com.xerxes.msme.admin;

import com.xerxes.msme.admin.dto.ImportDefaultCatalogRequest;
import com.xerxes.msme.admin.dto.ImportDefaultCatalogResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Bulk seeding/updating of the starter catalog - the app-facing replacement
 * for hand-writing a SQL migration every time it needs to grow. See
 * AdminDefaultProductCatalogService.importCatalog. Platform-staff-only, same
 * role gate as every other /api/admin/** path (SecurityConfig).
 */
@RestController
@RequestMapping("/api/admin/default-catalog")
public class AdminDefaultCatalogImportController {

    private final AdminDefaultProductCatalogService catalogService;

    public AdminDefaultCatalogImportController(AdminDefaultProductCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @PostMapping(path = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportDefaultCatalogResponse importCatalog(
            @Valid @RequestPart("manifest") ImportDefaultCatalogRequest manifest,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        Map<String, MultipartFile> imagesByFileName = images == null
                ? Map.of()
                : images.stream().collect(Collectors.toMap(MultipartFile::getOriginalFilename, Function.identity()));
        return catalogService.importCatalog(manifest, imagesByFileName);
    }
}
