package pas.matt.bielmatksef.model;

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

    public String getNip() {
        return nip;
    }

    public void setNip(String nip) {
        this.nip = nip;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getBuildingNumber() {
        return buildingNumber;
    }

    public void setBuildingNumber(String buildingNumber) {
        this.buildingNumber = buildingNumber;
    }

    public String getApartmentNumber() {
        return apartmentNumber;
    }

    public void setApartmentNumber(String apartmentNumber) {
        this.apartmentNumber = apartmentNumber;
    }


    public String getAddressKsef() {
        return addressKsef;
    }

    public void setAddressKsef(String addressKsef) {
        this.addressKsef = addressKsef;
    }
}
