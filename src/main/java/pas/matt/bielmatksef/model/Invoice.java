package pas.matt.bielmatksef.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class Invoice {

    private String invoiceNumber;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate issueDate;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
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
    private List<String> invoiceErrors = new ArrayList<>();

    private BigDecimal totalNet;
    private BigDecimal totalVat;
    private BigDecimal totalGross;

    private InvoicePayment payment;

}
