package gr.hua.dit.dras.entities;

/* imports */
import gr.hua.dit.dras.model.enums.ListingStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table
public class Listing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "date_scraped")
    private LocalDateTime dateScraped;

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
    @Max(200) //0-200 €/m2
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
    @Min(1)
    @Max(20)
    private Integer rooms;

    @NotBlank
    @Size(max = 50)
    @Column(nullable = false, length = 50)
    private String propertyType;

    @NotBlank
    @Size(max = 50)
    @Column(nullable = false, length = 50)
    private String rentalDuration;

    @Size(max = 500)
    @Column(length = 500)
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
    @ManyToMany(mappedBy = "appliedListings", cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH})
    private Set<Tenant> applicants = new HashSet<>();

    public Set<Tenant> getApplicants() {
        return applicants;
    }

    public void setApplicants(Set<Tenant> applicants) {
        this.applicants = applicants;
    }

    public Listing(
            Integer id,
            LocalDateTime dateScraped,
            String title,
            String subtitle,
            String description,
            Integer price,
            Integer pricePerM2,
            String address,
            Integer sizeM2,
            Integer rooms,
            String propertyType,
            String rentalDuration,
            String sourceUrl,
            ListingStatus status,
            boolean external,
            Owner owner,
            Tenant tenant
    ) {
        this.id = id;
        this.dateScraped=dateScraped;
        this.title = title;
        this.subtitle = subtitle;
        this.description = description;
        this.price = price;
        this.pricePerM2 = pricePerM2;
        this.address = address;
        this.sizeM2 = sizeM2;
        this.rooms = rooms;
        this.propertyType = propertyType;
        this.rentalDuration = rentalDuration;
        this.sourceUrl = sourceUrl;
        this.status = status;
        this.external = external;
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

    public LocalDateTime getDateScraped() {
        return dateScraped;
    }

    public void setDateScraped(LocalDateTime dateScraped) {
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

    public void markAsRented(Tenant tenant) {
        if (!isApproved()) {
            throw new IllegalStateException("Only approved listings can be rented");
        }
        if (tenant == null) {
            throw new IllegalArgumentException("Tenant cannot be null");
        }

        this.tenant = tenant;
        this.status = ListingStatus.RENTED;
    }

    @Override
    public String toString() {
        return "Listing{" +
                "id=" + id +
                ", dateScraped=" + dateScraped +
                ", title='" + title + '\'' +
                ", subtitle='" + subtitle + '\'' +
                ", description='" + description + '\'' +
                ", price=" + price +
                ", pricePerM2=" + pricePerM2 +
                ", address='" + address + '\'' +
                ", sizeM2=" + sizeM2 +
                ", rooms=" + rooms +
                ", propertyType='" + propertyType + '\'' +
                ", rentalDuration='" + rentalDuration + '\'' +
                ", sourceUrl='" + sourceUrl + '\'' +
                ", status=" + status +
                ", images=" + images +
                ", external=" + external +
                ", owner=" + owner +
                ", tenant=" + tenant +
                ", applicants=" + applicants +
                '}';
    }

}
