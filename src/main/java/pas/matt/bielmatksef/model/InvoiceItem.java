package pas.matt.bielmatksef.model;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
public class InvoiceItem {

    private int position;                   //pozycja na fakturze
    private String name;
    private String unit;                // np. "szt", "kg", "m"
    private BigDecimal quantity;        //ilość
    private BigDecimal unitNetPrice;    //jednostkowa cena netto
    private BigDecimal netValue;        //łączna cena netto pozycji (unitNetPrice * quantity)

    private String vatRate = "23";          // Przykładowo: "23", "8", "5", "0", "zw", "np"

}
