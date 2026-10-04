package gr.hua.dit.dras.dto;

import gr.hua.dit.dras.model.enums.PropertyType;
import gr.hua.dit.dras.model.enums.RentalDuration;
import jakarta.validation.constraints.*;
import java.util.List;

public class ListingCreateDTO {
    @NotBlank(message = "Title is required")
    @Size(max = 150, message = "Title cannot exceed 150 characters")
    private String title;

    @Size(max = 250, message = "Subtitle cannot exceed 250 characters")
    private String subtitle;

    @NotBlank(message = "Description is required")
    @Size(max = 5000, message = "Description cannot exceed 5000 characters")
    private String description;

    @NotNull(message = "Price is required")
    @Min(value = 0, message = "Price cannot be negative")
    @Max(value = 20000, message = "Price cannot exceed €20,000/month")
    private Integer price;

    @NotNull(message = "Price per m² is required")
    @Min(value = 0, message = "Price per m² cannot be negative")
    @Max(value = 200, message = "Price per m² cannot exceed €200")
    private Integer pricePerM2;

    @NotBlank(message = "Address is required")
    @Size(max = 255, message = "Address cannot exceed 255 characters")
    private String address;

    @NotNull(message = "Property type is required")
    private PropertyType propertyType;

    @NotNull(message = "Rental duration is required")
    private RentalDuration rentalDuration;

    @Min(value = 1900, message = "Year built cannot be before 1900")
    @Max(value = 2100, message = "Year built is invalid")
    private Integer yearBuilt;

    @NotNull(message = "Size is required")
    @Min(value = 5, message = "Size must be at least 5 m²")
    @Max(value = 1000, message = "Size cannot exceed 1000 m²")
    private Integer sizeM2;

    @Min(value = -3, message = "Floor cannot be below -3")
    @Max(value = 100, message = "Floor cannot exceed 100")
    private Integer floor;

    @Min(value = 0, message = "Bedrooms cannot be negative")
    @Max(value = 10, message = "Bedrooms cannot exceed 10")
    private Integer bedrooms;

    @Min(value = 0, message = "Bathrooms cannot be negative")
    @Max(value = 5, message = "Bathrooms cannot exceed 5")
    private Integer bathrooms;

    @Size(max = 10, message = "You can upload a maximum of 10 images")
    private List<String> images;

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

    public PropertyType getPropertyType() {
        return propertyType;
    }
    public void setPropertyType(PropertyType propertyType) {
        this.propertyType = propertyType;
    }

    public RentalDuration getRentalDuration() {
        return rentalDuration;
    }
    public void setRentalDuration(RentalDuration rentalDuration) {
        this.rentalDuration = rentalDuration;
    }

    public Integer getYearBuilt() {
        return yearBuilt;
    }
    public void setYearBuilt(Integer yearBuilt) {
        this.yearBuilt = yearBuilt;
    }

    public Integer getSizeM2() {
        return sizeM2;
    }
    public void setSizeM2(Integer sizeM2) {
        this.sizeM2 = sizeM2;
    }

    public List<String> getImages() {
        return images;
    }
    public void setImages(List<String> images) {
        this.images = images;
    }

    public Integer getFloor() {
        return floor;
    }
    public void setFloor(Integer floor) {
        this.floor = floor;
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
}
