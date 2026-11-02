package pas.matt.bielmatksef.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class InvoiceValidation {

    public static void validateInvoice(Invoice invoice) {

        validateInvoiceDetails(invoice);
        validateProducts(invoice);

        validateProductsSumWithSummary(invoice);
        validateSummary(invoice);
        validateProductsPricesNotZero(invoice);
        validateProductsNetValue(invoice);
    }

    private static void validateInvoiceDetails(Invoice invoice) {
        requireText(invoice.getCurrency(), "Brak waluty faktury", invoice);
        requireText(invoice.getInvoiceNumber(), "Brak numeru faktury", invoice);
        requirePresent(invoice.getIssueDate(), "Brak daty wystawienia faktury", invoice);
        requirePresent(invoice.getSaleDate(), "Brak daty sprzedaży", invoice);
        requireText(invoice.getPlaceOfIssue(), "Brak miejsca wystawienia faktury", invoice);
        requireText(invoice.getInvoiceType(), "Brak typu faktury", invoice);
        requirePresent(invoice.getTotalNet(), "Brak sumy netto", invoice);
        requirePresent(invoice.getTotalVat(), "Brak sumy VAT", invoice);
        requirePresent(invoice.getTotalGross(), "Brak sumy brutto", invoice);
        requireText(invoice.getPayment().getMethod(), "Brak metody płatności", invoice);
        validateParty(invoice.getSeller(), "Brak danych sprzedawcy: ", invoice);
        validateParty(invoice.getBuyer(), "Brak danych nabywcy: ", invoice);
    }

    private static void validateProducts(Invoice invoice) {
        List<InvoiceItem> items = invoice.getItems();

        for (int i = 0; i < items.size(); i++) {
            InvoiceItem item = items.get(i);
            String field = "Produkt poz [" + i + "] brak: ";
            requireText(item.getName(), field + "nazwy produktu", invoice);
            requireText(item.getUnit(), field + "jednostki", invoice);
            requirePresent(item.getQuantity(), field + "ilości", invoice);
            requirePresent(item.getUnitNetPrice(), field + "ceny jednostkowej netto", invoice);
            requirePresent(item.getNetValue(), field + "ceny łącznej netto", invoice);
            requireText(item.getVatRate(), field + "stawki VAT", invoice);
        }
    }

    private static void validateParty(InvoiceParty party, String field, Invoice invoice) {
        requireText(party.getNip(), field + "nip", invoice);
        requireText(party.getName(), field + "nazwa podmiotu", invoice);
        requireText(party.getAddressKsef(), field + "adres", invoice);
    }

    private static void requireText(String value, String field, Invoice invoice) {
        if (value == null || value.isBlank()) {
            invoice.getInvoiceErrors().add(field);
        }
    }

    private static void requirePresent(Object value, String field, Invoice invoice) {
        if (value == null) {
            invoice.getInvoiceErrors().add(field);
        }
    }

    private static void validateProductsSumWithSummary(Invoice invoice) {
        List<InvoiceItem> items = invoice.getItems();

        BigDecimal netValueSum = items.stream()
                .map(InvoiceItem::getNetValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netValueSumControl = netValueSum.setScale(2, RoundingMode.HALF_EVEN);

        if (!(netValueSumControl.compareTo(invoice.getTotalNet()) == 0)) {
            invoice.getInvoiceErrors().add("UWAGA! Suma łącznych cen netto produktów nie zgadza się z podsumowaniem");
        }
    }

    private static void validateSummary(Invoice invoice) {

        BigDecimal summaryNetto = invoice.getTotalNet();
        BigDecimal summaryVAT = invoice.getTotalVat();
        BigDecimal summaryBrutto = invoice.getTotalGross();

        BigDecimal summaryBruttoControl = summaryNetto.multiply(BigDecimal.valueOf(1.23));
        BigDecimal summaryVATControl = summaryNetto.multiply(BigDecimal.valueOf(0.23));

        BigDecimal summaryBruttoControlRounded = summaryBruttoControl.setScale(2, RoundingMode.HALF_UP);
        BigDecimal summaryVATControlRounded = summaryVATControl.setScale(2, RoundingMode.HALF_UP);


        if (!(summaryBrutto.compareTo(summaryBruttoControlRounded) == 0)
                || !(summaryVAT.compareTo(summaryVATControlRounded) == 0)) {
            invoice.getInvoiceErrors().add("UWAGA! Błąd w podsumowaniu. Sprawdź kwoty podsumowania");
        }
    }

    private static void validateProductsPricesNotZero(Invoice invoice) {  //czy cena produktu nie jest równa zero
        long productsPriceZero = invoice.getItems().stream()
                .filter(item -> item.getNetValue().compareTo(BigDecimal.ZERO) == 0)
                .count();

        if (productsPriceZero > 0) {
            invoice.getInvoiceErrors().add("UWAGA! Conajmniej jeden z produktów zawiera cenę ZERO");
        }
    }

    private static void validateProductsNetValue(Invoice invoice){
        invoice.getItems().stream()
                .filter(InvoiceValidation::productNetValueVerify)
                .forEach(item -> invoice.getInvoiceErrors().add
                        ("UWAGA! Produkt z pozycji " + item.getPosition() + " ma niezgodną ilość i cenę netto z wartością łączną netto"));
    }

    private static boolean productNetValueVerify(InvoiceItem item) {
        BigDecimal productNetValueToValidate = item.getQuantity().multiply(item.getUnitNetPrice())
                .setScale(2, RoundingMode.HALF_UP);

        return !(productNetValueToValidate.compareTo(item.getNetValue()) == 0);
    }

}
