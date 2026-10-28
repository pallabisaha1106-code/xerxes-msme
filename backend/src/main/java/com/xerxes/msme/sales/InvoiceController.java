package com.xerxes.msme.sales;

import com.xerxes.msme.sales.dto.InvoiceRequest;
import com.xerxes.msme.sales.dto.InvoiceResponse;
import com.xerxes.msme.security.AuthContext;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final InvoicePdfService invoicePdfService;
    private final AuthContext authContext;

    public InvoiceController(InvoiceService invoiceService, InvoicePdfService invoicePdfService, AuthContext authContext) {
        this.invoiceService = invoiceService;
        this.invoicePdfService = invoicePdfService;
        this.authContext = authContext;
    }

    @PostMapping
    public ResponseEntity<InvoiceResponse> create(@Valid @RequestBody InvoiceRequest request) {
        var principal = authContext.current();
        InvoiceResponse response = invoiceService.createInvoice(principal.tenantId(), principal.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<InvoiceResponse> list() {
        return invoiceService.listInvoices(authContext.current().tenantId());
    }

    @GetMapping("/{id}")
    public InvoiceResponse get(@PathVariable UUID id) {
        return invoiceService.getInvoice(authContext.current().tenantId(), id);
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable UUID id) {
        InvoicePdfService.InvoicePdf pdf = invoicePdfService.generateInvoicePdf(authContext.current().tenantId(), id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + pdf.filename() + "\"")
                .body(pdf.content());
    }
}
