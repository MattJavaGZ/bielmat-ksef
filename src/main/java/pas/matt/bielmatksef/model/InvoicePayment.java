package pas.matt.bielmatksef.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Setter
@Getter
public class InvoicePayment {

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dueDate;
    private String method;
    private String bankAccount;
    private String bankName;
    private Integer PaymentTermDays;

}
