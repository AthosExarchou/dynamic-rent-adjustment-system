package gr.hua.dit.dras.entities;

/* imports */
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(
        name = "tenant_listing_applications",
        uniqueConstraints = @UniqueConstraint(name = "uk_tenant_listing_application", columnNames = {"tenant_id", "listing_id"})
)
public class ListingApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;

    @Size(max = 500)
    @Column(name = "notes", length = 500)
    private String notes;

    @CreationTimestamp
    @Column(name = "applied_at", updatable = false)
    private Instant appliedAt = Instant.now();

    public ListingApplication() {
    }

    public ListingApplication(Tenant tenant, Listing listing) {
        this(tenant, listing, null);
    }

    public ListingApplication(Tenant tenant, Listing listing, String notes) {
        this.tenant = tenant;
        this.listing = listing;
        this.notes = notes != null ? notes.trim() : null;
        this.appliedAt = Instant.now();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public Listing getListing() {
        return listing;
    }

    public void setListing(Listing listing) {
        this.listing = listing;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes != null ? notes.trim() : null;
    }

    public Instant getAppliedAt() {
        return appliedAt;
    }

    public void setAppliedAt(Instant appliedAt) {
        this.appliedAt = appliedAt;
    }

    @PrePersist
    @PreUpdate
    private void sanitize() {
        if (this.notes != null) {
            String trimmed = this.notes.trim();
            this.notes = trimmed.isEmpty() ? null : trimmed;
        }
        if (this.appliedAt == null) {
            this.appliedAt = Instant.now();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ListingApplication that = (ListingApplication) o;
        if (id != null && that.id != null) {
            return Objects.equals(id, that.id);
        }
        return Objects.equals(tenant, that.tenant) && Objects.equals(listing, that.listing);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "ListingApplication{" +
                "id=" + id +
                ", tenant=" + (tenant != null ? tenant.getId() : null) +
                ", listing=" + (listing != null ? listing.getId() : null) +
                ", notes='" + notes + '\'' +
                ", appliedAt=" + appliedAt +
                '}';
    }
}
