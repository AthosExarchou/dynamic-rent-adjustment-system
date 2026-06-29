package gr.hua.dit.dras.dto;

import gr.hua.dit.dras.model.enums.PropertyType;
import gr.hua.dit.dras.model.enums.RentalDuration;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public class ListingCreateDTO {
    @NotBlank
    private String title;
    private String subtitle;
    @NotBlank
    private String description;
    @NotNull
    @Positive
    private Integer price;
    @NotNull
    @Positive
    private Integer pricePerM2;
    @NotBlank
    private String address;
    @NotNull
    private PropertyType propertyType;
    @NotNull
    private RentalDuration rentalDuration;
    private Integer yearBuilt;
    @NotNull
    @Positive
    private Integer sizeM2;
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
}
