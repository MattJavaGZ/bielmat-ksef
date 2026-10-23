package pas.matt.bielmatksef.model;

import java.math.BigDecimal;

public class InvoiceItem {

    private int position;                   //pozycja na fakturze
    private String name;
    private String unit;                // np. "szt", "kg", "m"
    private BigDecimal quantity;        //ilość
    private BigDecimal unitNetPrice;    //jednostkowa cena netto
    private BigDecimal netValue;        //łączna cena netto pozycji (unitNetPrice * quantity)

    private String vatRate = "23";          // Przykładowo: "23", "8", "5", "0", "zw", "np"

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitNetPrice() {
        return unitNetPrice;
    }

    public void setUnitNetPrice(BigDecimal unitNetPrice) {
        this.unitNetPrice = unitNetPrice;
    }

    public BigDecimal getNetValue() {
        return netValue;
    }

    public void setNetValue(BigDecimal netValue) {
        this.netValue = netValue;
    }

    public String getVatRate() {
        return vatRate;
    }

    public void setVatRate(String vatRate) {
        this.vatRate = vatRate;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }
}
