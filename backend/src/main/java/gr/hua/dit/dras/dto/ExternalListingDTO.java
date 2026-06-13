package gr.hua.dit.dras.dto;

/* imports */
import gr.hua.dit.dras.model.validation.ValidYearBuilt;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.ArrayList;

public class ExternalListingDTO {

    @NotBlank(message = "Title is mandatory")
    @Size(max = 150)
    private String title;

    @Size(max = 250)
    private String subtitle;

    @NotBlank(message = "Description is mandatory")
    @Size(max = 5000)
    private String description;

    @NotNull(message = "Price is mandatory")
    @Min(value = 0, message = "Price cannot be negative")
    @Max(20000)
    private Integer price;

    @Min(value = 0, message = "Price per M2 cannot be negative")
    @Max(200)
    private Integer pricePerM2;

    @NotBlank(message = "Address is mandatory")
    @Size(max = 255)
    private String address;

    @NotNull(message = "Size in M2 is mandatory")
    @Min(value = 5, message = "Size must be at least 5 sq meters")
    @Max(1000)
    private Integer sizeM2;

    @Min(value = -3)
    @Max(100)
    private Integer floor;

    @ValidYearBuilt
    private Integer yearBuilt;

    @Min(value = 0)
    @Max(10)
    private Integer bedrooms;

    @Min(value = 0)
    @Max(5)
    private Integer bathrooms;

    private String propertyType;

    private String rentalDuration;

    @NotBlank(message = "Source URL is mandatory")
    @Size(max = 500)
    private String sourceUrl;

    private List<String> images = new ArrayList<>();

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

    public Integer getFloor() {
        return floor;
    }

    public void setFloor(Integer floor) {
        this.floor = floor;
    }

    public Integer getYearBuilt() {
        return yearBuilt;
    }

    public void setYearBuilt(Integer yearBuilt) {
        this.yearBuilt = yearBuilt;
    }

    public Integer getBedrooms() {
        return bedrooms;
    }

    public void setBedrooms(Integer bedrooms) {
        this.bedrooms = bedrooms;
    }

    public Integer getBathrooms() {
        return bathrooms;
    }

    public void setBathrooms(Integer bathrooms) {
        this.bathrooms = bathrooms;
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

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }

}
