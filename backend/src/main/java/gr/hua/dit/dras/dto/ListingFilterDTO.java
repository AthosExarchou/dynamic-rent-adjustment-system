package gr.hua.dit.dras.dto;

/* imports */
import gr.hua.dit.dras.model.enums.PropertyType;
import java.time.LocalDate;

public class ListingFilterDTO {

    private String title;

    private Integer minPrice;
    private Integer maxPrice;

    private PropertyType type;

    private String municipality;
    private String district;

    private Integer minBedrooms;
    private Integer maxBedrooms;

    private Integer minBathrooms;
    private Integer maxBathrooms;

    private Integer minYear;
    private Integer maxYear;

    private LocalDate updatedAfter;
    private LocalDate updatedBefore;

    private Boolean externalOnly;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDate getUpdatedBefore() {
        return updatedBefore;
    }

    public void setUpdatedBefore(LocalDate updatedBefore) {
        this.updatedBefore = updatedBefore;
    }

    public LocalDate getUpdatedAfter() {
        return updatedAfter;
    }

    public void setUpdatedAfter(LocalDate updatedAfter) {
        this.updatedAfter = updatedAfter;
    }

    public Integer getMaxYear() {
        return maxYear;
    }

    public void setMaxYear(Integer maxYear) {
        this.maxYear = maxYear;
    }

    public Integer getMinYear() {
        return minYear;
    }

    public void setMinYear(Integer minYear) {
        this.minYear = minYear;
    }

    public Integer getMaxBathrooms() {
        return maxBathrooms;
    }

    public void setMaxBathrooms(Integer maxBathrooms) {
        this.maxBathrooms = maxBathrooms;
    }

    public Integer getMinBathrooms() {
        return minBathrooms;
    }

    public void setMinBathrooms(Integer minBathrooms) {
        this.minBathrooms = minBathrooms;
    }

    public Integer getMaxBedrooms() {
        return maxBedrooms;
    }

    public void setMaxBedrooms(Integer maxBedrooms) {
        this.maxBedrooms = maxBedrooms;
    }

    public Integer getMinBedrooms() {
        return minBedrooms;
    }

    public void setMinBedrooms(Integer minBedrooms) {
        this.minBedrooms = minBedrooms;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getMunicipality() {
        return municipality;
    }

    public void setMunicipality(String municipality) {
        this.municipality = municipality;
    }

    public PropertyType getType() {
        return type;
    }

    public void setType(PropertyType type) {
        this.type = type;
    }

    public Integer getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(Integer maxPrice) {
        this.maxPrice = maxPrice;
    }

    public Integer getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(Integer minPrice) {
        this.minPrice = minPrice;
    }

    public Boolean getExternalOnly() {
        return externalOnly;
    }

    public void setExternalOnly(Boolean externalOnly) {
        this.externalOnly = externalOnly;
    }
}
