package moadian;

import ir.gov.tax.tpis.sdk.content.dto.InvoiceDto;

public class InvoiceRequest {
    private InvoiceDto invoice;
    private String vendorName;
    private String username;

    public InvoiceRequest(InvoiceDto invoice, String vendorName, String username) {
        this.invoice = invoice;
        this.vendorName = vendorName;
        this.username = username;
    }

    public InvoiceDto getInvoice() {
        return invoice;
    }

    public String getVendorName() {
        return vendorName;
    }

    public String getUsername() {
        return username;
    }

    // getters and setters
}
