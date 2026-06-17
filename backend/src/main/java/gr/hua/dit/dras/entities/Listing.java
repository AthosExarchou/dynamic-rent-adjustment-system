package gr.hua.dit.dras.entities;

/* imports */
import gr.hua.dit.dras.model.enums.ListingStatus;
import gr.hua.dit.dras.model.enums.PropertyType;
import gr.hua.dit.dras.model.enums.RentalDuration;
import jakarta.persistence.*;
import gr.hua.dit.dras.model.validation.ValidYearBuilt;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "listings")
public class Listing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Version
    private Integer version;

    @Column(name = "date_scraped")
    private Instant dateScraped;

    @NotBlank
    @Size(max = 150)
    @Column(nullable = false, length = 150)
    private String title;

    @Size(max = 250)
    @Column(length = 250)
    private String subtitle;

    @NotBlank
    @Size(max = 5000)
    @Column(nullable = false, length = 5000)
    private String description;

    @NotNull
    @Min(0)
    @Max(20000)
    @Column(nullable = false)
    private Integer price;

    @NotNull
    @Min(0)
    @Max(200) // 0-200 €/m2
    @Column(nullable = false)
    private Integer pricePerM2;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false)
    private String address;

    @NotNull
    @Min(5)
    @Max(1000)
    @Column(nullable = false)
    private Integer sizeM2;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PropertyType propertyType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RentalDuration rentalDuration;

    @Min(-3)
    @Max(100)
    @Column
    private Integer floor;

    @ValidYearBuilt
    @Column
    private Integer yearBuilt;

    @Min(0)
    @Max(10)
    @Column
    private Integer bedrooms;

    @Min(0)
    @Max(5)
    @Column
    private Integer bathrooms;

    @Size(max = 500)
    @Column(name = "source_url",unique = true, length = 500)
    private String sourceUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ListingStatus status = ListingStatus.PENDING;

    @ElementCollection
    @CollectionTable(
            name = "listing_images",
            joinColumns = @JoinColumn(name = "listing_id")
    )
    @Column(name = "image_url", length = 1000)
    private List<String> images = new ArrayList<>();

    @Column(nullable = false)
    private boolean external = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    /* Listing-Owner relationship */
    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE,
            CascadeType.DETACH, CascadeType.REFRESH})
    @JoinColumn(name = "owner_id")
    private Owner owner;

    /* Listing-Tenant relationship */
    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE,
            CascadeType.DETACH, CascadeType.REFRESH})
    @JoinColumn(name = "tenant_id", referencedColumnName = "id", unique = true)
    private Tenant tenant;

    /* Tenant applications relationship */
    @ManyToMany(mappedBy = "appliedListings", cascade = {
            CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH
    })
    private Set<Tenant> applicants = new HashSet<>();

    public Listing(
            Integer id,
            Instant dateScraped,
            String title,
            String subtitle,
            String description,
            Integer price,
            Integer pricePerM2,
            String address,
            Integer sizeM2,
            PropertyType propertyType,
            RentalDuration rentalDuration,
            Integer floor,
            Integer yearBuilt,
            Integer bedrooms,
            Integer bathrooms,
            String sourceUrl,
            ListingStatus status,
            boolean external,
            Instant createdAt,
            Instant updatedAt,
            Owner owner,
            Tenant tenant
    ) {
        this.id = id;
        this.dateScraped = dateScraped;
        this.title = title;
        this.subtitle = subtitle;
        this.description = description;
        this.price = price;
        this.pricePerM2 = pricePerM2;
        this.address = address;
        this.sizeM2 = sizeM2;
        this.propertyType = propertyType;
        this.rentalDuration = rentalDuration;
        this.floor = floor;
        this.yearBuilt = yearBuilt;
        this.bedrooms = bedrooms;
        this.bathrooms = bathrooms;
        this.sourceUrl = sourceUrl;
        this.status = status;
        this.external = external;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.owner = owner;
        this.tenant = tenant;
    }

    public Listing() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Instant getDateScraped() {
        return dateScraped;
    }

    public void setDateScraped(Instant dateScraped) {
        this.dateScraped = dateScraped;
    }

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

    public Owner getOwner() {
        return owner;
    }

    public void setOwner(Owner owner) {
        this.owner = owner;
    }

    public ListingStatus getStatus() {
        return status;
    }

    public void setStatus(ListingStatus status) {
        this.status = status;
    }

    public boolean isApproved() {
        return status == ListingStatus.APPROVED;
    }

    public boolean isPending() {
        return status == ListingStatus.PENDING;
    }

    public boolean isDisabled() {
        return status == ListingStatus.DISABLED;
    }

    public boolean isRejected() {
        return status == ListingStatus.REJECTED;
    }

    public boolean isRented() {
        return status == ListingStatus.RENTED;
    }

    public boolean isExternal() {
        return external;
    }

    public void setExternal(boolean external) {
        this.external = external;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Set<Tenant> getApplicants() {
        return applicants;
    }

    public void setApplicants(Set<Tenant> applicants) {
        this.applicants = applicants;
    }

    public void addApplicant(Tenant tenant) {
        if (!applicants.contains(tenant)) {
            applicants.add(tenant);
            tenant.getAppliedListings().add(this);
        }
    }

    public void disable() {
        if (this.status == ListingStatus.RENTED) {
            throw new IllegalStateException("Cannot disable rented listing");
        }
        this.status = ListingStatus.DISABLED;
    }

    public void approve() {
        if (!isPending()) {
            throw new IllegalStateException("Only pending listings can be approved");
        }
        this.status = ListingStatus.APPROVED;
    }

    public void reject() {
        if (!isPending()) {
            throw new IllegalStateException("Only pending listings can be rejected");
        }
        this.status = ListingStatus.REJECTED;
    }



    public void makeAvailable() {
        if (status == ListingStatus.RENTED) {
            throw new IllegalStateException("Cannot make rented listing available");
        }
        this.status = ListingStatus.APPROVED;
    }

    public void removeApplicant(Tenant tenant) {
        this.applicants.remove(tenant); // removes tenant from listing's applicant list
        tenant.getAppliedListings().remove(this); // removes listing from tenant's applied listings
    }

    /**
     * Domain Logic: The listing manages its own rental state.
     */
    public List<Tenant> rentTo(Tenant winningTenant) {

        if (!applicants.contains(winningTenant)) {
            throw new IllegalStateException("Tenant did not apply");
        }

        if (this.tenant != null) {
            throw new IllegalStateException("Listing is already rented.");
        }

        this.tenant = winningTenant;
        this.status = ListingStatus.RENTED;

        winningTenant.rent(this);

        List<Tenant> rejected = new ArrayList<>(this.applicants);
        rejected.remove(winningTenant);

        // Sync inverse side: remove this listing from each applicant's appliedListings
        for (Tenant applicant : this.applicants) {
            applicant.getAppliedListings().remove(this);
        }
        this.applicants.clear();

        return rejected;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        Listing listing = (Listing) o;
        return id != null && id.equals(listing.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Listing{" +
                "id=" + id +
                ", dateScraped=" + dateScraped +
                ", title='" + title + '\'' +
                ", price=" + price +
                ", pricePerM2=" + pricePerM2 +
                ", address='" + address + '\'' +
                ", sizeM2=" + sizeM2 +
                ", floor=" + floor +
                ", yearBuilt=" + yearBuilt +
                ", bedrooms=" + bedrooms +
                ", bathrooms=" + bathrooms +
                ", propertyType=" + propertyType +
                ", rentalDuration=" + rentalDuration +
                ", sourceUrl='" + sourceUrl + '\'' +
                ", status=" + status +
                ", external=" + external +
                ", createdAt=" + createdAt +
                '}';
    }

    /* Automatic Sanitization */
    @PrePersist
    @PreUpdate
    private void sanitize() {
        this.title = safeTrim(this.title);
        this.subtitle = safeTrim(this.subtitle);
        this.description = safeTrim(this.description);
        this.address = safeTrim(this.address);
        this.sourceUrl = safeTrim(this.sourceUrl);
    }

    private String safeTrim(String s) {
        return s == null ? null : s.trim();
    }

}
