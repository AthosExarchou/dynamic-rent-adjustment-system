package gr.hua.dit.dras.dto;

/* imports */
import gr.hua.dit.dras.model.enums.PropertyType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class PriceSuggestionRequestDTO {

    @NotNull(message = "Size in m² is required for price estimation")
    @Min(value = 5, message = "Size must be at least 5 m²")
    @Max(value = 1000, message = "Size cannot exceed 1000 m²")
    private Integer sizeM2;

    private PropertyType propertyType;

    private String address;

    @Min(value = -3, message = "Floor cannot be less than -3")
    @Max(value = 100, message = "Floor cannot exceed 100")
    private Integer floor;

    @Min(value = 1900, message = "Year built must be 1900 or later")
    @Max(value = 2100, message = "Invalid year built")
    private Integer yearBuilt;

    @Min(value = 0, message = "Bedrooms cannot be negative")
    @Max(value = 10, message = "Bedrooms cannot exceed 10")
    private Integer bedrooms;

    @Min(value = 0, message = "Bathrooms cannot be negative")
    @Max(value = 5, message = "Bathrooms cannot exceed 5")
    private Integer bathrooms;

    public PriceSuggestionRequestDTO() {
    }

    public PriceSuggestionRequestDTO(Integer sizeM2, PropertyType propertyType, String address) {
        this.sizeM2 = sizeM2;
        this.propertyType = propertyType;
        this.address = address;
    }

    public Integer getSizeM2() {
        return sizeM2;
    }

    public void setSizeM2(Integer sizeM2) {
        this.sizeM2 = sizeM2;
    }

    public PropertyType getPropertyType() {
        return propertyType;
    }

    public void setPropertyType(PropertyType propertyType) {
        this.propertyType = propertyType;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
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
}
