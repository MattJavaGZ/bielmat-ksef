package pas.matt.bielmatksef.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Invoice {

    private String invoiceNumber;
    private LocalDate issueDate;
    private LocalDate saleDate;
    private String currency = "PLN";
    private String placeOfIssue = "Bielsko-Biała";
    private String invoiceType = "VAT";
    private String info;
    private boolean splitPayment;

    private InvoiceParty seller;
    private InvoiceParty buyer;

    private List<InvoiceItem> items = new ArrayList<>();
    private List<String> unrecognizedItems = new ArrayList<>();

    private BigDecimal totalNet;
    private BigDecimal totalVat;
    private BigDecimal totalGross;

    private InvoicePayment payment;

    public BigDecimal getTotalGross() {
        return totalGross;
    }

    public void setTotalGross(BigDecimal totalGross) {
        this.totalGross = totalGross;
    }

    public BigDecimal getTotalVat() {
        return totalVat;
    }

    public void setTotalVat(BigDecimal totalVat) {
        this.totalVat = totalVat;
    }

    public BigDecimal getTotalNet() {
        return totalNet;
    }

    public void setTotalNet(BigDecimal totalNet) {
        this.totalNet = totalNet;
    }

    public List<InvoiceItem> getItems() {
        return items;
    }

    public void setItems(List<InvoiceItem> items) {
        this.items = items;
    }

    public List<String> getUnrecognizedItems() {
        return unrecognizedItems;
    }

    public void setUnrecognizedItems(List<String> unrecognizedItems) {
        this.unrecognizedItems = unrecognizedItems;
    }

    public InvoiceParty getBuyer() {
        return buyer;
    }

    public void setBuyer(InvoiceParty buyer) {
        this.buyer = buyer;
    }

    public InvoiceParty getSeller() {
        return seller;
    }

    public void setSeller(InvoiceParty seller) {
        this.seller = seller;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public LocalDate getSaleDate() {
        return saleDate;
    }

    public void setSaleDate(LocalDate saleDate) {
        this.saleDate = saleDate;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public InvoicePayment getPayment() {
        return payment;
    }

    public void setPayment(InvoicePayment payment) {
        this.payment = payment;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String getPlaceOfIssue() {
        return placeOfIssue;
    }

    public void setPlaceOfIssue(String placeOfIssue) {
        this.placeOfIssue = placeOfIssue;
    }

    public String getInvoiceType() {
        return invoiceType;
    }

    public void setInvoiceType(String invoiceType) {
        this.invoiceType = invoiceType;
    }

    public boolean isSplitPayment() {
        return splitPayment;
    }

    public void setSplitPayment(boolean splitPayment) {
        this.splitPayment = splitPayment;
    }
}
