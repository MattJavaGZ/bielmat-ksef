package pas.matt.bielmatksef.xml;

import org.springframework.stereotype.Component;
import pas.matt.bielmatksef.model.Invoice;
import pas.matt.bielmatksef.model.InvoiceItem;
import pas.matt.bielmatksef.model.InvoiceParty;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.ByteArrayOutputStream;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Component
public class XmlBuilder {

    private static final String NS = "http://crd.gov.pl/wzor/2025/06/25/13775/";

    public byte[] generateXml(Invoice invoice) throws XMLStreamException {

        validateInvoice(invoice);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        XMLOutputFactory factory = XMLOutputFactory.newFactory();

        factory.setProperty(
                XMLOutputFactory.IS_REPAIRING_NAMESPACES,
                true
        );

        XMLStreamWriter xml = factory.createXMLStreamWriter(output, "UTF-8");
        xml.writeStartDocument("UTF-8", "1.0");
        xml.setDefaultNamespace(NS);
        xml.writeStartElement("", "Faktura", NS);
//        xml.writeDefaultNamespace(NS);

        xml.writeNamespace(
                "etd",
                "http://crd.gov.pl/xml/schematy/dziedzinowe/mf/2022/01/05/eD/DefinicjeTypy/"
        );
        xml.writeNamespace(
                "xsi",
                "http://www.w3.org/2001/XMLSchema-instance"
        );

        createHeading(xml);
        createSeller(invoice, xml);
        createBuyer(invoice, xml);
        createFa(invoice, xml);
        createFooter(invoice, xml);

        xml.writeEndElement();
        xml.writeEndDocument();
        xml.flush();
        xml.close();

        return output.toByteArray();
    }


    private static void createFooter(Invoice invoice, XMLStreamWriter xml) throws XMLStreamException {

        if (invoice.getInfo() == null || invoice.getInfo().isBlank()) {
            return;
        }

        xml.writeStartElement("Stopka");
        xml.writeStartElement("Informacje");
        xml.writeStartElement("StopkaFaktury");

        xml.writeCharacters(invoice.getInfo());

        xml.writeEndElement();
        xml.writeEndElement();
        xml.writeEndElement();
    }


    private static void createFa(Invoice invoice, XMLStreamWriter xml) throws XMLStreamException {
        xml.writeStartElement("Fa");
        xml.writeStartElement("KodWaluty");
        xml.writeCharacters(invoice.getCurrency());
        xml.writeEndElement();

        xml.writeStartElement("P_1");
        xml.writeCharacters(invoice.getIssueDate().toString());
        xml.writeEndElement();

        xml.writeStartElement("P_1M");
        xml.writeCharacters(invoice.getPlaceOfIssue());
        xml.writeEndElement();

        xml.writeStartElement("P_2");
        xml.writeCharacters(invoice.getInvoiceNumber());
        xml.writeEndElement();

        xml.writeStartElement("P_6");
        xml.writeCharacters(invoice.getSaleDate().toString());
        xml.writeEndElement();

        xml.writeStartElement("P_13_1");
        xml.writeCharacters(invoice.getTotalNet().toString());
        xml.writeEndElement();

        xml.writeStartElement("P_14_1");
        xml.writeCharacters(invoice.getTotalVat().toString());
        xml.writeEndElement();

        xml.writeStartElement("P_15");
        xml.writeCharacters(invoice.getTotalGross().toString());
        xml.writeEndElement();

        // Adnotacje — przed RodzajFaktury
        generateAnnotation(invoice, xml);

        xml.writeStartElement("RodzajFaktury");
        xml.writeCharacters(invoice.getInvoiceType());
        xml.writeEndElement();

        // produkty
        generateInvoiceItems(invoice, xml);

        //płatność
        generateInvoicePayment(invoice, xml);

        xml.writeEndElement(); // Fa
    }

    private static void generateAnnotation(Invoice invoice, XMLStreamWriter xml) throws XMLStreamException {
        xml.writeStartElement("Adnotacje");

        xml.writeStartElement("P_16");
        xml.writeCharacters("2");
        xml.writeEndElement();

        xml.writeStartElement("P_17");
        xml.writeCharacters("2");
        xml.writeEndElement();

        xml.writeStartElement("P_18");
        xml.writeCharacters("2");
        xml.writeEndElement();

        xml.writeStartElement("P_18A");
        xml.writeCharacters(invoice.isSplitPayment() ? "1" : "2");
        xml.writeEndElement();

        xml.writeStartElement("Zwolnienie");
        xml.writeStartElement("P_19N");
        xml.writeCharacters("1");
        xml.writeEndElement();
        xml.writeEndElement();

        xml.writeStartElement("NoweSrodkiTransportu");
        xml.writeStartElement("P_22N");
        xml.writeCharacters("1");
        xml.writeEndElement();
        xml.writeEndElement();

        xml.writeStartElement("P_23");
        xml.writeCharacters("2");
        xml.writeEndElement();

        xml.writeStartElement("PMarzy");
        xml.writeStartElement("P_PMarzyN");
        xml.writeCharacters("1");
        xml.writeEndElement();
        xml.writeEndElement();

        xml.writeEndElement(); // Adnotacje
    }

    private static void generateInvoicePayment(Invoice invoice, XMLStreamWriter xml) throws XMLStreamException {

        if (invoice.getPayment() != null) {
            xml.writeStartElement("Platnosc");

            if (invoice.getPayment().getDueDate() != null) {
                xml.writeStartElement("TerminPlatnosci");

                xml.writeStartElement("Termin");
                xml.writeCharacters(invoice.getPayment().getDueDate().toString());
                xml.writeEndElement();
                    //dodatkowy opis płatności
                    if (invoice.getPayment().getPaymentTermDays() != null) {
                        xml.writeStartElement("TerminOpis");

                        xml.writeStartElement("Ilosc");
                        xml.writeCharacters(String.valueOf(invoice.getPayment().getPaymentTermDays()));
                        xml.writeEndElement();
                        xml.writeStartElement("Jednostka");
                        xml.writeCharacters("dni");
                        xml.writeEndElement();
                        xml.writeStartElement("ZdarzeniePoczatkowe");
                        xml.writeCharacters("Przelew");
                        xml.writeEndElement();

                        xml.writeEndElement();
                    }

                xml.writeEndElement();
            }

            String paymentMethod = invoice.getPayment().getMethod();
            if (paymentMethod != null && !paymentMethod.isBlank()) {
                int paymentMethodCode = getPaymentMethodCode(paymentMethod);
                if (paymentMethodCode != 0) {
                    xml.writeStartElement("FormaPlatnosci");
                    xml.writeCharacters(String.valueOf(paymentMethodCode));
                    xml.writeEndElement();
                }

//                xml.writeStartElement("OpisPlatnosci");
//                xml.writeCharacters(paymentMethod);
//                xml.writeEndElement();
            }

            String account = invoice.getPayment().getBankAccount();

            if (account != null && !account.isBlank()) {
                xml.writeStartElement("RachunekBankowy");

                xml.writeStartElement("NrRB");
                xml.writeCharacters(account);
                xml.writeEndElement();

                String bankName = invoice.getPayment().getBankName();

                if (bankName != null && !bankName.isBlank()) {
                    xml.writeStartElement("NazwaBanku");
                    xml.writeCharacters(bankName);
                    xml.writeEndElement();
                }

                xml.writeEndElement(); // RachunekBankowy
            }

            xml.writeEndElement(); // Platnosc
        }
    }

    private static int getPaymentMethodCode(String paymentMethod) {
        if (paymentMethod.toLowerCase().contains("przelew")) return 6;
        if (paymentMethod.toLowerCase().contains("gotówk") || paymentMethod.toLowerCase().contains("gotowk")) return 1;
        if (paymentMethod.toLowerCase().contains("karta")) return 2;
        return 0;
    }

    private static void generateInvoiceItems(Invoice invoice, XMLStreamWriter xml) throws XMLStreamException {
        List<InvoiceItem> items = invoice.getItems();
        for (InvoiceItem item : items) {
            xml.writeStartElement("FaWiersz");

            xml.writeStartElement("NrWierszaFa");
            xml.writeCharacters(String.valueOf(item.getPosition()));
            xml.writeEndElement();

            xml.writeStartElement("P_7");
            xml.writeCharacters(item.getName());
            xml.writeEndElement();

            xml.writeStartElement("P_8A");
            xml.writeCharacters(item.getUnit());
            xml.writeEndElement();

            xml.writeStartElement("P_8B");
            xml.writeCharacters(item.getQuantity().toString());
            xml.writeEndElement();

            xml.writeStartElement("P_9A");
            xml.writeCharacters(item.getUnitNetPrice().toString());
            xml.writeEndElement();

            xml.writeStartElement("P_11");
            xml.writeCharacters(item.getNetValue().toString());
            xml.writeEndElement();

            xml.writeStartElement("P_12");
            xml.writeCharacters(item.getVatRate());
            xml.writeEndElement();

            xml.writeEndElement();
        }
    }

    private static void createHeading(XMLStreamWriter xml) throws XMLStreamException {
        xml.writeStartElement("Naglowek");

        xml.writeStartElement("KodFormularza");
        xml.writeAttribute("kodSystemowy", "FA (3)");
        xml.writeAttribute("wersjaSchemy", "1-0E");
        xml.writeCharacters("FA");
        xml.writeEndElement();

        xml.writeStartElement("WariantFormularza");
        xml.writeCharacters("3");
        xml.writeEndElement();

        xml.writeStartElement("DataWytworzeniaFa");
        xml.writeCharacters(OffsetDateTime.now(ZoneOffset.UTC).toString());
        xml.writeEndElement();

        xml.writeStartElement("SystemInfo");
        xml.writeCharacters("BielmatKsef");
        xml.writeEndElement();

        xml.writeEndElement();
    }

    private static void createSeller(Invoice invoice, XMLStreamWriter xml) throws XMLStreamException {
        xml.writeStartElement("Podmiot1");

        xml.writeStartElement("DaneIdentyfikacyjne");

        xml.writeStartElement("NIP");
        xml.writeCharacters(invoice.getSeller().getNip());
        xml.writeEndElement();

        xml.writeStartElement("Nazwa");
        xml.writeCharacters(invoice.getSeller().getName());
        xml.writeEndElement();

        xml.writeEndElement();

        xml.writeStartElement("Adres");

        xml.writeStartElement("KodKraju");
        xml.writeCharacters(invoice.getSeller().getCountryCode());
        xml.writeEndElement();

        xml.writeStartElement("AdresL1");
        xml.writeCharacters(invoice.getSeller().getAddressKsef());
        xml.writeEndElement();

//        xml.writeStartElement("AdresL1");
//        xml.writeCharacters(invoice.getSeller().getStreet() + " " + invoice.getSeller().getBuildingNumber()
//                + (invoice.getSeller().getApartmentNumber() == null ? "" : "/" + invoice.getSeller().getApartmentNumber()));
//        xml.writeEndElement();
//
//        xml.writeStartElement("AdresL2");
//        xml.writeCharacters(invoice.getSeller().getPostalCode() + " " + invoice.getSeller().getCity());
//        xml.writeEndElement();

        xml.writeEndElement();

        xml.writeEndElement();
    }


    private static void createBuyer(Invoice invoice, XMLStreamWriter xml) throws XMLStreamException {
        InvoiceParty buyer = invoice.getBuyer();

        xml.writeStartElement("Podmiot2");

        xml.writeStartElement("DaneIdentyfikacyjne");
//
//        if (buyer.getNip() != null && !buyer.getNip().isEmpty()) {
//            xml.writeStartElement("NIP");
//            xml.writeCharacters(buyer.getNip());
//            xml.writeEndElement();
//        } else {
//            xml.writeStartElement("BrakID");
//            xml.writeCharacters("1");
//            xml.writeEndElement();
//        }

        xml.writeStartElement("NIP");
        xml.writeCharacters(buyer.getNip());
        xml.writeEndElement();

        xml.writeStartElement("Nazwa");
        xml.writeCharacters(buyer.getName());
        xml.writeEndElement();

        xml.writeEndElement();

        xml.writeStartElement("Adres");

        xml.writeStartElement("KodKraju");
        xml.writeCharacters(buyer.getCountryCode());
        xml.writeEndElement();

        xml.writeStartElement("AdresL1");
        xml.writeCharacters(buyer.getAddressKsef());
        xml.writeEndElement();

//        xml.writeStartElement("AdresL1");
//        xml.writeCharacters(buyer.getStreet() + " " + buyer.getBuildingNumber()
//                + (buyer.getApartmentNumber() == null ? "" : "/" + buyer.getApartmentNumber()));
//        xml.writeEndElement();
//
//        xml.writeStartElement("AdresL2");
//        xml.writeCharacters(buyer.getPostalCode() + " " + buyer.getCity());
//        xml.writeEndElement();

        xml.writeEndElement();

        xml.writeStartElement("JST");
        xml.writeCharacters("2");
        xml.writeEndElement();

        xml.writeStartElement("GV");
        xml.writeCharacters("2");
        xml.writeEndElement();

        xml.writeEndElement();

    }

    private static void validateInvoice(Invoice invoice) {
            requirePresent(invoice, "invoice");
            requireText(invoice.getCurrency(), "invoice.currency");
            requireText(invoice.getInvoiceNumber(), "invoice.invoiceNumber");
            requirePresent(invoice.getIssueDate(), "invoice.issueDate");
            requirePresent(invoice.getSaleDate(), "invoice.saleDate");
            requireText(invoice.getPlaceOfIssue(), "invoice.placeOfIssue");
            requireText(invoice.getInvoiceType(), "invoice.invoiceType");
            requirePresent(invoice.getTotalNet(), "invoice.totalNet");
            requirePresent(invoice.getTotalVat(), "invoice.totalVat");
            requirePresent(invoice.getTotalGross(), "invoice.totalGross");
        validateParty(invoice.getSeller(), "invoice.seller");
        validateParty(invoice.getBuyer(), "invoice.buyer");

    }

    private static void validateParty(InvoiceParty party, String field) {
        requirePresent(party, field);
        requireText(party.getNip(), field + ".nip");
        requireText(party.getName(), field + ".name");
        requireText(party.getAddressKsef(), field + ".adres");
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Brak wymaganej wartości: " + field);
        }
    }

    private static void requirePresent(Object value, String field) {
        if (value == null) {
            throw new IllegalArgumentException("Brak wymaganej wartości: " + field);
        }
    }


}
