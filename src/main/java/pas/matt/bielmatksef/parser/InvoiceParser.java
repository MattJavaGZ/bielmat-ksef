package pas.matt.bielmatksef.parser;

import org.springframework.stereotype.Service;
import pas.matt.bielmatksef.model.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class InvoiceParser {

    // Początek pozycji: numer pozycji + pięciocyfrowy kod towaru
    private static final Pattern ITEM_START = Pattern.compile(
            "(?<!\\S)(\\d{1,2})[ \\t]+(\\d{5})[ \\t]+"
    );

    // Ilość, jednostka, cena przed rabatem, rabat,
    // cena po rabacie, wartość netto, stawka VAT.
    private static final Pattern ITEM_TAIL = Pattern.compile(
            "(\\d+(?:,\\d+)?)\\s+" +
                    "(SZT\\.?|OPAK\\.?|OP\\.?|PARA\\.?|PAR\\.?|ROLK\\.?|MB\\.?|M\\.?|M2\\.?|M3\\.?|MIKR\\.?|KG\\.?|T\\.?" +
                    "|TONA\\.?|KM\\.?|KOMPLET\\.?|KPL\\.?|KMPL\\.?|KOMP\\.?|KWH\\.?|L\\.?|LITR\\.?|USŁU\\.?|ZEST\\.?)\\s+" +   //+OPAK, KOMPLET, KPL, KMPL
                    "(\\d[\\d.]*,\\d{2})" +
                    "(?:\\s+(\\d[\\d.]*,\\d{2}))?" +
                    "\\s+(\\d[\\d.]*,\\d{2})" +
                    "\\s+(\\d[\\d.]*,\\d{2})" +
                    "\\s+(23|8|5|0)\\b"
    );
    //Data wystawienia, data sprzedaży.
    private static final Pattern DATES = Pattern.compile(
            "Data wystawienia\\s+Data sprzedaży\\s+" +
                    "(\\d{2}\\.\\d{2}\\.\\d{4})\\s+" +
                    "(\\d{2}\\.\\d{2}\\.\\d{4})",
            Pattern.CASE_INSENSITIVE
    );
    // Podsumowanie faktury: suma netto, suma VAT, suma brutto.
    private static final Pattern TOTTALS = Pattern.compile(
            "Razem:\\s*([\\d.]+,\\d{2})\\s+" +
                    "([\\d.]+,\\d{2})\\s+" +
                    "([\\d.]+,\\d{2})",
            Pattern.CASE_INSENSITIVE
    );

    public Invoice parseInvoice(String text) {
        Invoice invoice = new Invoice();

        // Numer faktury
        invoice.setInvoiceNumber(findFirst(
                text,
                "Faktura\\s+(F\\s*/\\s*S\\d+/\\d+/\\d+)"
        ));

        // Data wystawienia i data sprzedaży
        Matcher dates = DATES.matcher(text);

        if (dates.find()) {
            invoice.setIssueDate(parseDate(dates.group(1)));
            invoice.setSaleDate(parseDate(dates.group(2)));
        }

        //splitpayment
        invoice.setSplitPayment(
                text.matches("(?is).*mechanizm\\s+podzielnej\\s+płatności.*")
        );

//        //Sprzedawca, nabywca
//        parseParties(text, invoice);
        generateSeller(invoice);
        parseBuyer(text, invoice);

        // Informacje dodatkowe, np. numery WZ
        invoice.setInfo(parseInfo(text));

        // Podsumowanie faktury
        Matcher totals = TOTTALS.matcher(text);

        if (totals.find()) {
            invoice.setTotalNet(parseDecimal(totals.group(1)));
            invoice.setTotalVat(parseDecimal(totals.group(2)));
            invoice.setTotalGross(parseDecimal(totals.group(3)));
        }

        // Płatność
        parsePayment(text, invoice);

        // Pozycje — używamy działającego parsera
        List<String> unrecognizedItems = new ArrayList<>();
        invoice.setItems(parseItems(text, unrecognizedItems));
        invoice.setUnrecognizedItems(unrecognizedItems);

        InvoiceValidation.validateInvoice(invoice);

        return invoice;
    }

    private void parsePayment(String text, Invoice invoice) {
        InvoicePayment payment = new InvoicePayment();

//        payment.setMethod(findFirst(
//                text,
//                "(?i)Forma płatności\\s+(Przelew(?:\\s+\\d+\\s+dni)?|KARTA|gotówka|gotówką)"
//        ));

        payment.setMethod(findFirst(
                text,
                "(?iu)(?:Forma\\s+płatności\\s*:?\\s*|ZAPŁACONO\\s+)?(Przelew(?:\\s+\\d+\\s+dni)?|KARTA|gotówk[ąa]|gotowk[ąa])"
        ));

        String paymentTermDays = findFirst(
                text,
                "(?iu)Przelew\\s+(\\d+)\\s+dni"
        );

        if (paymentTermDays != null) {
            payment.setPaymentTermDays(Integer.parseInt(paymentTermDays));
        }

        String dueDate = findFirst(
                text,
                "Termin płatności\\s+(\\d{2}\\.\\d{2}\\.\\d{4})"
        );

        if (dueDate != null) {
            payment.setDueDate(parseDate(dueDate));
        }

        payment.setBankAccount(findFirst(
                text,
                "Konto\\s+(\\d{26})"
        ));
        payment.setBankName(findFirst(
                text,
                "Bank\\s+([^\\r\\n]+?)(?=\\s+Konto\\b|\\s+\\d{26}\\b)"
        ));
        invoice.setPayment(payment);
    }

    private String findFirst(String text, String regex) {
        Matcher matcher = Pattern.compile(
                regex,
                Pattern.CASE_INSENSITIVE
        ).matcher(text);

        return matcher.find() ? matcher.group(1).trim() : null;
    }

    private LocalDate parseDate(String value) {
        return LocalDate.parse(
                value,
                DateTimeFormatter.ofPattern("dd.MM.uuuu")
        );
    }

    private String parseInfo(String text) {
        Pattern pattern = Pattern.compile(
                "WZ\\s*/\\s*S\\d+/\\d+/\\d+:"
                        + ".*?(?=\\s*(?:KSEF\\s*:|Symbol\\s+Nazwa\\s+towaru/usługi)|$)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );

        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            return matcher.group()
                    .replaceAll("\\s+", " ")
                    .trim();
        }

        return null;
    }

    private List<InvoiceItem> parseItems(String text, List<String> unrecognizedItems) {
        List<InvoiceItem> items = new ArrayList<>();

        Matcher starts = ITEM_START.matcher(text);
        List<Integer> positions = new ArrayList<>();
//        List<Integer> numbers = new ArrayList<>();

//        while (starts.find()) {
//            int number = Integer.parseInt(starts.group(1));
//            positions.add(starts.start());
//            numbers.add(number);
//        }

        int lastItemNumber = 0;

        while (starts.find()) {
            int number = Integer.parseInt(starts.group(1));

            // Numery pozycji muszą rosnąć.
            // Zapobiega to potraktowaniu "23 38421"
            // na końcu pozycji 35 jako nowej pozycji 23.
            if (number <= lastItemNumber) {
                continue;
            }

            positions.add(starts.start());
//            numbers.add(number);
            lastItemNumber = number;
        }

        for (int i = 0; i < positions.size(); i++) {

            int start = positions.get(i);
            int end = (i + 1 < positions.size())
                    ? positions.get(i + 1)
                    : text.length();

            String block = text.substring(start, end);

            Matcher header = ITEM_START.matcher(block);

            if (!header.find()) {
                continue;
            }

            int itemNumber = Integer.parseInt(header.group(1));
            String productCode = header.group(2);

            String content = block.substring(header.end());

            Matcher tail = ITEM_TAIL.matcher(content);

            if (!tail.find()) {
                if (unrecognizedItems != null) {
                    unrecognizedItems.add(
                            "Nie rozpoznano pozycji " + itemNumber
                                    + ", kod: " + productCode
                    );
                }
                continue;
            }

            String name = content.substring(0, tail.start()).trim();
            String suffix = content.substring(tail.end()).trim();

// Zachowaj kod PKWiU, nawet jeśli znajduje się w kolejnej linii.
            Matcher pkwiuMatcher = Pattern.compile(
                    "(?i)PKWiU\\s*:\\s*([\\d.]+-[\\d.]+)"
            ).matcher(content);

            String pkwiu = pkwiuMatcher.find()
                    ? "PKWiU: " + pkwiuMatcher.group(1)
                    : null;

// Usuń PKWiU z suffix, żeby nie dodać go drugi raz.
            suffix = suffix.replaceAll(
                    "(?i)PKWiU\\s*:\\s*[\\d.]+-[\\d.]+",
                    ""
            ).trim();

// Usuń symbole regału, które OCR umieścił za danymi liczbowymi.
            suffix = suffix.replaceAll(
                    "(?<!\\S)(?:B\\d{4}|A\\d{4}|C\\d{4}|E\\d{2}[A-Z]{2}-\\d+)(?!\\S)",
                    " "
            ).replaceAll("\\s+", " ").trim();

            name = appendProductSuffix(name, suffix);

            if (pkwiu != null && !name.toLowerCase().contains("pkwiu:")) {
                name = name + " " + pkwiu;
            }

            name = name.replaceAll("\\s+", " ").trim();

            InvoiceItem item = new InvoiceItem();
            item.setPosition(itemNumber);
            item.setName(name);
            item.setQuantity(parseDecimal(tail.group(1)));
            item.setUnit(tail.group(2).replace(".", ""));
            item.setUnitNetPrice(parseDecimal(tail.group(5)));
            item.setNetValue(parseDecimal(tail.group(6)));
//            item.setVatRate(tail.group(7));

            items.add(item);
        }

        return items;
    }

    private String appendProductSuffix(String name, String suffix) {
        if (suffix == null || suffix.isBlank()) {
            return name;
        }

        String cleaned = suffix.trim();

        // Odrzuć nagłówki i stopki stron faktury.
        if (cleaned.matches("(?is).*Program\\s+EuroFirma\\s+PRO.*")
                || cleaned.matches("(?is).*Ciąg\\s+dalszy\\s+dokumentu.*")
                || cleaned.matches("(?is).*Strona\\s+\\d+.*")
                || cleaned.matches("(?is).*Symbol\\s+Nazwa\\s+towaru.*")
                || cleaned.matches("(?is).*Płatność\\s*:.*")
                || cleaned.matches("(?is).*DO\\s+ZAPŁATY\\s*:.*")) {
            return name;
        }

        // Zachowaj dodatkowe oznaczenie produktu, np. PKWiU,
        // kod kreskowy lub symbol katalogowy F2.0339.
        return (name + " " + cleaned)
                .replaceAll("\\s+", " ")
                .trim();
    }



    private void parseBuyer(String text, Invoice invoice) {
        String normalizedText = text.replace('\u00A0', ' ');
//        String[] lines = normalizedText.split("\\R");

        InvoiceParty buyer = new InvoiceParty();

        // 1. Znajdź nazwę nabywcy przed adresem sprzedawcy.
        // Nazwa sprzedawcy służy wyłącznie jako punkt odniesienia.
        Pattern buyerPrefixPattern = Pattern.compile(
                "BIELMAT\\s+Sp\\.\\s*z\\s*o\\.\\s*o\\.\\s+(.+?)" +
                        "(?=\\s+\\d{2}-\\d{3}\\s+.+?\\s+ul\\.?\\s*)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );

        String buyerName = null;
        Matcher prefixMatcher = buyerPrefixPattern.matcher(normalizedText);

        if (prefixMatcher.find()) {
            StringBuilder fullName = new StringBuilder(
                    prefixMatcher.group(1).trim()
            );

            // 2. Sprawdź tekst bezpośrednio po adresie sprzedawcy.
            // Jeśli zaczyna się od kodu pocztowego, jest to adres nabywcy,
            // a nie dalsza część jego nazwy.
            Pattern sellerAddressPattern = Pattern.compile(
                    "\\d{2}-\\d{3}\\s+.+?\\s+ul\\.?\\s*.+?\\s+\\d+[A-Za-z]?",
                    Pattern.CASE_INSENSITIVE
            );

            Matcher sellerAddressMatcher = sellerAddressPattern.matcher(
                    normalizedText.substring(prefixMatcher.start(1))
            );

            if (sellerAddressMatcher.find()) {
                int continuationStart =
                        prefixMatcher.start(1) + sellerAddressMatcher.end();

                String afterSellerAddress =
                        normalizedText.substring(continuationStart).trim();

                // Kontynuacja nazwy może być przed NIP-em nabywcy.
                // Nie dopisuj kodu pocztowego, NIP-u ani REGON-u.
                if (!afterSellerAddress.isEmpty()
                        && !afterSellerAddress.matches(
                        "(?is)^(?:\\d{2}-\\d{3}\\b|NIP\\s*:|REGON\\s*:|TEL\\.?\\b|FAX\\.?\\b).*"
                )) {

                    String continuation = afterSellerAddress.split(
                            "(?i)\\s+(?=NIP\\s*:|REGON\\s*:|Tel\\.?\\b|Fax\\.?\\b|www\\.|E-mail\\s*:|Symbol\\s+Nazwa\\s+towaru|KSEF\\s*:)",
                            2
                    )[0].trim();

                    // Jeżeli tekst po adresie sprzedawcy zaczyna się od
                    // nazwy, a nie od adresu, dołącz go do nazwy nabywcy.
                    if (!continuation.isEmpty()
                            && !continuation.matches("(?is)^\\d{2}-\\d{3}\\b.*")) {
                        fullName.append(" ").append(continuation);
                    }
                }
            }

            buyerName = fullName.toString()
                    .replaceAll("\\s+", " ")
                    .trim();
        }

        // 3. NIP nabywcy: drugi NIP w dokumencie.
        Matcher nipMatcher = Pattern.compile(
                "NIP\\s*:\\s*(?:PL\\s*)?(\\d{10})",
                Pattern.CASE_INSENSITIVE
        ).matcher(normalizedText);

        if (nipMatcher.find() && nipMatcher.find()) {
            buyer.setNip(nipMatcher.group(1));
        }

        // 4. Kod pocztowy i miasto nabywcy.

            // Wariant A: kod pocztowy i miasto rozdzielone danymi sprzedawcy.
            // Kod pocztowy jest przed NIP-em sprzedawcy,
            // a miasto znajduje się za REGON-em sprzedawcy.
        Pattern buyerCitySeparatedBySellerDataPattern = Pattern.compile(
                "\\d{2}-\\d{3}\\s+.+?\\s+ul\\.?\\s*.+?\\s+\\d+[A-Za-z]?" +
                        "\\s+(\\d{2}-\\d{3})" +
                        "\\s+NIP\\s*:\\s*(?:PL\\s*)?\\d{10}" +
                        "\\s+Regon\\s*:\\s*\\d+\\s+(.+?)" +
                        "(?=\\s+(?:Tel\\.?\\b|Fax\\.?\\b))",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );

        Matcher separatedCityMatcher =
                buyerCitySeparatedBySellerDataPattern.matcher(normalizedText);

        if (separatedCityMatcher.find()) {
            buyer.setPostalCode(separatedCityMatcher.group(1).trim());
            buyer.setCity(separatedCityMatcher.group(2).trim());

        } else {
            // Wariant B: kod pocztowy i miasto w jednym wierszu.
            Pattern buyerCitySameLinePattern = Pattern.compile(
                    "\\d{2}-\\d{3}\\s+.+?\\s+ul\\.?\\s*.+?\\s+\\d+[A-Za-z]?" +
                            "\\s+(\\d{2}-\\d{3})\\s+(.+?)" +
                            "(?=\\s+(?:NIP\\s*:|REGON\\s*:|Tel\\.?\\b|Fax\\.?\\b))",
                    Pattern.CASE_INSENSITIVE
            );

            Matcher cityMatcher = buyerCitySameLinePattern.matcher(normalizedText);

            if (cityMatcher.find()) {
                buyer.setPostalCode(cityMatcher.group(1).trim());
                buyer.setCity(cityMatcher.group(2).trim());

            } else {
                // Wariant C: kod pocztowy i miasto występują po REGON-ie sprzedawcy.
                Pattern buyerCityAfterRegonPattern = Pattern.compile(
                        "Regon\\s*:\\s*\\d+\\s+(\\d{2}-\\d{3})\\s+(.+?)" +
                                "(?=\\s+(?:Tel\\.?\\b|ul\\.?\\b|NIP\\s*:|Fax\\.?\\b))",
                        Pattern.CASE_INSENSITIVE
                );

                Matcher cityAfterRegonMatcher =
                        buyerCityAfterRegonPattern.matcher(normalizedText);

                if (cityAfterRegonMatcher.find()) {
                    buyer.setPostalCode(cityAfterRegonMatcher.group(1).trim());
                    buyer.setCity(cityAfterRegonMatcher.group(2).trim());
                }
            }
        }


        // 5. Ulica nabywcy.

        // Wariant A: ulica występuje po kodzie pocztowym i numerach telefonu.
        Pattern buyerStreetAfterPhonePattern = Pattern.compile(
                "Regon\\s*:\\s*\\d+\\s+\\d{2}-\\d{3}\\s+.+?" +
                        "\\s+Tel\\.?[^\\r\\n]*?\\s+ul\\.?\\s*([^\\r\\n]+)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );

        Matcher streetMatcher =
                buyerStreetAfterPhonePattern.matcher(normalizedText);

        if (streetMatcher.find()) {
            String street = streetMatcher.group(1)
                    .replaceAll(
                            "(?i)\\s+(?:Fax\\.?|NIP\\s*:|www\\.|E-mail\\s*:|Symbol\\b).*$",
                            ""
                    )
                    .trim();

            setStreetAndBuilding(buyer, "ul. " + street);

        } else {

            // Wariant B: ulica nabywcy występuje bezpośrednio po REGON-ie.
            Pattern buyerStreetAfterRegonPattern = Pattern.compile(
                    "Regon\\s*:\\s*\\d+\\s+ul\\.?\\s*([^\\r\\n]+)",
                    Pattern.CASE_INSENSITIVE
            );

            Matcher streetAfterRegonMatcher =
                    buyerStreetAfterRegonPattern.matcher(normalizedText);

            if (streetAfterRegonMatcher.find()) {
                String street = streetAfterRegonMatcher.group(1)
                        .replaceAll(
                                "(?i)\\s+(?:Fax\\.?|NIP\\s*:|www\\.|E-mail\\s*:|Symbol\\b).*$",
                                ""
                        )
                        .trim();

                setStreetAndBuilding(buyer, "ul. " + street);

            } else {

                // Wariant C: ulica nabywcy występuje po numerach telefonów.
                Pattern buyerStreetAfterPhoneFallbackPattern = Pattern.compile(
                        "Tel\\.?[\\s\\S]*?\\bul\\.?\\s*([^\\r\\n]+)",
                        Pattern.CASE_INSENSITIVE
                );

                Matcher streetAfterPhoneFallbackMatcher =
                        buyerStreetAfterPhoneFallbackPattern.matcher(normalizedText);

                if (streetAfterPhoneFallbackMatcher.find()) {
                    String street = streetAfterPhoneFallbackMatcher.group(1)
                            .replaceAll(
                                    "(?i)\\s+(?:Fax\\.?|NIP\\s*:|www\\.|E-mail\\s*:|Symbol\\b).*$",
                                    ""
                            )
                            .trim();

                    setStreetAndBuilding(buyer, "ul. " + street);
                }
            }
        }

        buyer.setName(buyerName);
        buyer.setAddressKsef(buyer.generateAddressKsef());

        // Zabezpieczenie: NIP sprzedawcy nie może znaleźć się w adresie nabywcy.
//        String sellerNip = invoice.getSeller().getNip();
//
//        if (sellerNip != null && !sellerNip.isBlank()) {
//            if (buyer.getAddressKsef() != null
//                    && buyer.getAddressKsef().replaceAll("\\D", "")
//                    .contains(sellerNip.replaceAll("\\D", ""))) {
//                buyer.setAddressKsef(null);
//            }
//        }

        invoice.setBuyer(buyer);
    }

    private void generateSeller(Invoice invoive) {
        InvoiceParty seller = new InvoiceParty();
        seller.setNip("5472084567");
        seller.setName("BIELMAT SPÓŁKA Z OGRANICZONĄ ODPOWIEDZIALNOŚCIĄ");
        seller.setPostalCode("43-300");
        seller.setCity("Bielsko-Biała");
        seller.setStreet("Warszawska");
        seller.setBuildingNumber("56");
        seller.setAddressKsef(seller.generateAddressKsef());
        invoive.setSeller(seller);
    }


    private void setStreetAndBuilding(InvoiceParty party, String address) {
        Matcher matcher = Pattern.compile(
                "ul\\.?\\s*(.*?)\\s+(\\d+[A-Za-z]?)\\s*$",
                Pattern.CASE_INSENSITIVE
        ).matcher(address.trim());

        if (matcher.find()) {
            party.setStreet(matcher.group(1).trim());
            party.setBuildingNumber(matcher.group(2));
        }
    }


    private BigDecimal parseDecimal(String value) {
        return new BigDecimal(
                value.replace(".", "").replace(',', '.')
        );
    }

    //    private void parseParties(String text, Invoice invoice) {
//        String[] lines = text.split("\\R");
//
//        // Znajdź wiersz z nazwami sprzedawcy i nabywcy.
//        String sellerName = null;
//        String buyerName = null;
//
//        Pattern namesPattern = Pattern.compile(
//                "^(.+?\\b(?:Sp\\.\\s*z\\s*o\\.\\s*o\\.|S\\.\\s*A\\.))\\s+(.+)$",
//                Pattern.CASE_INSENSITIVE
//        );
//
//        for (int i = 0; i < lines.length - 1; i++) {
//            if (lines[i].contains("SPRZEDAWCA")
//                    && lines[i].contains("NABYWCA")) {
//
//                Matcher names = namesPattern.matcher(lines[i + 1].trim());
//
//                if (names.find()) {
//                    sellerName = names.group(1).trim();
//                    buyerName = names.group(2).trim();
//                }
//                break;
//            }
//        }
//
//        InvoiceParty seller = new InvoiceParty();
//        InvoiceParty buyer = new InvoiceParty();
//
//        seller.setName(sellerName);
//        buyer.setName(buyerName);
//
//        // Odczytaj numery NIP w kolejności występowania na fakturze.
//        Matcher nipMatcher = Pattern.compile(
//                "NIP:\\s*(?:PL)?\\s*(\\d{10})",
//                Pattern.CASE_INSENSITIVE
//        ).matcher(text);
//
//        if (nipMatcher.find()) {
//            seller.setNip(nipMatcher.group(1));
//        }
//
//        if (nipMatcher.find()) {
//            buyer.setNip(nipMatcher.group(1));
//        }
//
//        // Adresy: wiersz zawierający oba kody pocztowe.
//        Pattern addressesPattern = Pattern.compile(
//                "^(\\d{2}-\\d{3})\\s+(.+?)\\s+" +
//                        "(ul\\.?\\s*.+?\\s+\\d+[A-Za-z]?)\\s+" +
//                        "(\\d{2}-\\d{3})\\s+(.+?)\\s*$",
//                Pattern.CASE_INSENSITIVE
//        );
//
//        for (String line : lines) {
//            Matcher addresses = addressesPattern.matcher(line.trim());
//
//            if (addresses.find()) {
//                seller.setPostalCode(addresses.group(1));
//                seller.setCity(addresses.group(2).trim());
//                setStreetAndBuilding(seller, addresses.group(3));
//
//                buyer.setPostalCode(addresses.group(4));
//                buyer.setCity(addresses.group(5).trim());
//                break;
//            }
//        }
//
//        // Ulica nabywcy występuje po numerze REGON.
//        Pattern buyerStreetPattern = Pattern.compile(
//                "Regon:\\s*\\d+\\s+ul\\.?\\s*([^\\r\\n]+)",
//                Pattern.CASE_INSENSITIVE
//        );
//
//        Matcher buyerStreetMatcher = buyerStreetPattern.matcher(text);
//
//        if (buyerStreetMatcher.find()) {
//            setStreetAndBuilding(buyer, "ul. " +
//                    buyerStreetMatcher.group(1).trim());
//        }
//
//        seller.setAddressKsef(seller.generateAddressKsef());
//        buyer.setAddressKsef(buyer.generateAddressKsef());
//
//        invoice.setSeller(seller);
//        invoice.setBuyer(buyer);
//    }

}
