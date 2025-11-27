package gr.hua.dit.dras.dto;

public class ExternalListingDTO {

    private String title;
    private String subtitle;
    private String description;
    private Integer price;
    private Integer pricePerM2;
    private String address;
    private Integer sizeM2;
    private Integer rooms;
    private String propertyType;
    private String rentalDuration;
    private String sourceUrl;

    /* getters and setters */
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getPrice() {
        return price;
    }

    public void setPrice(Integer price) {
        this.price = price;
    }

    public Integer getPricePerM2() {
        return pricePerM2;
    }

    public void setPricePerM2(Integer pricePerM2) {
        this.pricePerM2 = pricePerM2;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Integer getSizeM2() {
        return sizeM2;
    }

    public void setSizeM2(Integer sizeM2) {
        this.sizeM2 = sizeM2;
    }

    public Integer getRooms() {
        return rooms;
    }

    public void setRooms(Integer rooms) {
        this.rooms = rooms;
    }

    public String getPropertyType() {
        return propertyType;
    }

    public void setPropertyType(String propertyType) {
        this.propertyType = propertyType;
    }

    public String getRentalDuration() {
        return rentalDuration;
    }

    public void setRentalDuration(String rentalDuration) {
        this.rentalDuration = rentalDuration;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }
}
