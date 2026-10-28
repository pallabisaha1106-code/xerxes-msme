package com.xerxes.msme.sales;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.xerxes.msme.catalog.Product;
import com.xerxes.msme.catalog.ProductRepository;
import com.xerxes.msme.catalog.ProductVariant;
import com.xerxes.msme.catalog.ProductVariantRepository;
import com.xerxes.msme.catalog.VariantAttributeResolver;
import com.xerxes.msme.common.exception.ResourceNotFoundException;
import com.xerxes.msme.tenant.Tenant;
import com.xerxes.msme.tenant.TenantRepository;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Renders a GST-compliant tax invoice as a PDF: an HTML string built from the
 * already-persisted invoice figures (never recomputed here - GST math is
 * InvoiceService's job) is handed to openhtmltopdf for layout. Runs
 * read-only since it's just a view over existing data.
 */
@Service
public class InvoicePdfService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MMM-yyyy");

    private final TenantRepository tenantRepository;
    private final CustomerRepository customerRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineItemRepository invoiceLineItemRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductRepository productRepository;

    public InvoicePdfService(
            TenantRepository tenantRepository,
            CustomerRepository customerRepository,
            InvoiceRepository invoiceRepository,
            InvoiceLineItemRepository invoiceLineItemRepository,
            ProductVariantRepository variantRepository,
            ProductRepository productRepository) {
        this.tenantRepository = tenantRepository;
        this.customerRepository = customerRepository;
        this.invoiceRepository = invoiceRepository;
        this.invoiceLineItemRepository = invoiceLineItemRepository;
        this.variantRepository = variantRepository;
        this.productRepository = productRepository;
    }

    public record InvoicePdf(String filename, byte[] content) {}

    @Transactional(readOnly = true)
    public InvoicePdf generateInvoicePdf(UUID tenantId, UUID invoiceId) {
        Invoice invoice = invoiceRepository
                .findByIdAndTenantId(invoiceId, tenantId)
                .orElseThrow(() -> ResourceNotFoundException.of("Invoice", invoiceId));
        Tenant tenant = tenantRepository
                .findById(tenantId)
                .orElseThrow(() -> new IllegalStateException("Authenticated request references a missing tenant"));
        Customer customer = customerRepository
                .findByIdAndTenantId(invoice.getCustomerId(), tenantId)
                .orElseThrow(() -> ResourceNotFoundException.of("Customer", invoice.getCustomerId()));

        String html = buildHtml(tenant, customer, invoice, invoiceLineItemRepository.findAllByInvoiceId(invoice.getId()));
        String filename = invoice.getInvoiceDisplayNumber().replace('/', '-') + ".pdf";
        return new InvoicePdf(filename, renderToPdf(html));
    }

    String buildHtml(Tenant tenant, Customer customer, Invoice invoice, Iterable<InvoiceLineItem> lineItems) {
        StringBuilder rows = new StringBuilder();
        int index = 1;
        for (InvoiceLineItem lineItem : lineItems) {
            ProductVariant variant = variantRepository
                    .findByIdAndTenantId(lineItem.getVariantId(), tenant.getId())
                    .orElseThrow(() -> ResourceNotFoundException.of("ProductVariant", lineItem.getVariantId()));
            Product product = productRepository
                    .findByIdAndTenantId(variant.getProductId(), tenant.getId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Product", variant.getProductId()));

            String description = product.getName()
                    + (variant.getAttributes().isEmpty()
                            ? ""
                            : " (" + String.join(", ", describeAttributes(variant)) + ")");
            BigDecimal taxableValue = lineItem.getQuantity().multiply(lineItem.getUnitPrice());

            rows.append("<tr>")
                    .append("<td>").append(index++).append("</td>")
                    .append("<td>").append(escape(description)).append("<br/><span class=\"sku\">SKU: ")
                    .append(escape(variant.getSku())).append("</span></td>")
                    .append("<td>").append(escape(VariantAttributeResolver.resolveHsnCode(product, variant))).append("</td>")
                    .append("<td class=\"text-right\">").append(lineItem.getQuantity()).append("</td>")
                    .append("<td class=\"text-right\">").append(money(lineItem.getUnitPrice())).append("</td>")
                    .append("<td class=\"text-right\">").append(money(taxableValue)).append("</td>")
                    .append("<td class=\"text-right\">").append(amountOrDash(lineItem.getCgstAmount())).append("</td>")
                    .append("<td class=\"text-right\">").append(amountOrDash(lineItem.getSgstAmount())).append("</td>")
                    .append("<td class=\"text-right\">").append(amountOrDash(lineItem.getIgstAmount())).append("</td>")
                    .append("<td class=\"text-right\">").append(money(lineItem.getLineTotal())).append("</td>")
                    .append("</tr>");
        }

        return """
                <html>
                <head>
                <style>
                  @page { size: A4; margin: 18mm 14mm; }
                  body { font-family: Helvetica, Arial, sans-serif; font-size: 10.5px; color: #1a1a1a; }
                  .invoice-title { text-align: center; font-size: 16px; font-weight: bold; text-transform: uppercase;
                    letter-spacing: 1px; margin-bottom: 12px; }
                  table { width: 100%%; border-collapse: collapse; }
                  .header-table td { vertical-align: top; padding-bottom: 10px; }
                  .biz-name { font-size: 14px; font-weight: bold; }
                  .box { border: 1px solid #999; padding: 8px; margin-bottom: 10px; }
                  .items-table { margin-bottom: 10px; }
                  .items-table th, .items-table td { border: 1px solid #999; padding: 5px; }
                  .items-table th { background: #f0f0f0; text-transform: uppercase; font-size: 9px; }
                  .sku { color: #666; font-size: 9px; }
                  .text-right { text-align: right; }
                  .totals-table { width: 260px; margin-left: auto; }
                  .totals-table td { padding: 3px 6px; }
                  .grand-total td { font-weight: bold; font-size: 12px; border-top: 1px solid #999; }
                  .words-row { margin-top: 8px; font-style: italic; }
                  .footer-note { margin-top: 24px; font-size: 9px; color: #666; text-align: center; }
                </style>
                </head>
                <body>
                  <div class="invoice-title">Tax Invoice</div>
                  <table class="header-table">
                    <tr>
                      <td style="width: 60%%;">
                        <div class="biz-name">%s</div>
                        <div>%s</div>
                        <div>%s %s</div>
                        <div>GSTIN: %s</div>
                      </td>
                      <td style="width: 40%%;" class="text-right">
                        <div>Invoice #: <strong>%s</strong></div>
                        <div>Invoice Date: %s</div>
                        <div>Place of Supply: %s</div>
                      </td>
                    </tr>
                  </table>

                  <div class="box">
                    <strong>Bill To</strong><br/>
                    %s<br/>
                    %s<br/>
                    %s %s<br/>
                    GSTIN: %s | State Code: %s
                  </div>

                  <table class="items-table">
                    <thead>
                      <tr>
                        <th>#</th><th>Item</th><th>HSN</th><th>Qty</th><th>Rate</th><th>Taxable Value</th>
                        <th>CGST</th><th>SGST</th><th>IGST</th><th>Total</th>
                      </tr>
                    </thead>
                    <tbody>
                      %s
                    </tbody>
                  </table>

                  <table class="totals-table">
                    <tr><td>Subtotal</td><td class="text-right">Rs. %s</td></tr>
                    <tr><td>CGST</td><td class="text-right">Rs. %s</td></tr>
                    <tr><td>SGST</td><td class="text-right">Rs. %s</td></tr>
                    <tr><td>IGST</td><td class="text-right">Rs. %s</td></tr>
                    <tr class="grand-total"><td>Grand Total</td><td class="text-right">Rs. %s</td></tr>
                  </table>

                  <div class="words-row">Amount in words: %s</div>

                  <div class="footer-note">This is a system-generated invoice and does not require a signature.</div>
                </body>
                </html>
                """
                .formatted(
                        escape(tenant.getBusinessName()),
                        escape(joinAddress(tenant.getAddressLine1(), tenant.getAddressLine2())),
                        escape(nullToEmpty(tenant.getCity())),
                        escape(nullToEmpty(tenant.getPincode())),
                        escape(nullToEmpty(tenant.getGstin())),
                        escape(invoice.getInvoiceDisplayNumber()),
                        invoice.getInvoiceDate().format(DATE_FORMAT),
                        escape(invoice.getPlaceOfSupplyStateCode()),
                        escape(customer.getName()),
                        escape(joinAddress(customer.getAddressLine1(), customer.getAddressLine2())),
                        escape(nullToEmpty(customer.getCity())),
                        escape(nullToEmpty(customer.getPincode())),
                        escape(nullToEmpty(customer.getGstin(), "Unregistered")),
                        escape(nullToEmpty(customer.getStateCode())),
                        rows,
                        money(invoice.getSubtotal()),
                        money(invoice.getCgstAmount()),
                        money(invoice.getSgstAmount()),
                        money(invoice.getIgstAmount()),
                        money(invoice.getTotalAmount()),
                        escape(AmountInWordsConverter.toWords(invoice.getTotalAmount())));
    }

    private byte[] renderToPdf(String html) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(outputStream);
            builder.run();
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to render invoice PDF", e);
        }
    }

    private static java.util.List<String> describeAttributes(ProductVariant variant) {
        return variant.getAttributes().entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue())
                .toList();
    }

    private static String joinAddress(String line1, String line2) {
        if (line1 == null || line1.isBlank()) return nullToEmpty(line2);
        if (line2 == null || line2.isBlank()) return line1;
        return line1 + ", " + line2;
    }

    private static String amountOrDash(BigDecimal amount) {
        return amount == null || amount.signum() == 0 ? "-" : money(amount);
    }

    private static String money(BigDecimal amount) {
        return amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String nullToEmpty(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
