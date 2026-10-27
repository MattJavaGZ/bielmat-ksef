package pas.matt.bielmatksef.model;

import java.util.List;

public class InvoiceValidation {

      public static void validateInvoice(Invoice invoice) {

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
//        requireText(party.getCountryCode(), field + "kod kraju", invoice);
//        requireText(party.getStreet(), field + ".street", invoice);
//        requireText(party.getBuildingNumber(), field + ".buildingNumber");
//        requireText(party.getPostalCode(), field + "kod pocztowy", invoice);
//        requireText(party.getCity(), field + "miasto", invoice);
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
}
