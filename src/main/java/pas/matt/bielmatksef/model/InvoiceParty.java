package pas.matt.bielmatksef.model;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class InvoiceParty {

    private String nip;
    private String name;
    private String countryCode = "PL";
    private String postalCode;
    private String city;
    private String street;
    private String buildingNumber;
    private String apartmentNumber;
    private String addressKsef;

    public String generateAddressKsef() {
        StringBuilder address = new StringBuilder();
        if (street != null && !street.isEmpty()) {
            address.append(street).append(" ");
        }
        if (buildingNumber != null && !buildingNumber.isEmpty()) {
            address.append(buildingNumber);
            if (apartmentNumber == null || apartmentNumber.isEmpty()) {
                address.append(", ");
            }
            if (apartmentNumber != null && !apartmentNumber.isEmpty()) {
                address.append("/").append(apartmentNumber).append(", ");
            }
        }
        if (postalCode != null && !postalCode.isEmpty()) {
            address.append(postalCode).append(" ");
        }
        if (city != null && !city.isEmpty()) {
            address.append(city);
        }

        return address.toString().trim();
    }

    public InvoiceParty() {
    }


}
