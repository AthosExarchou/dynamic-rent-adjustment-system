package gr.hua.dit.dras.entities;

/* imports */
import gr.hua.dit.dras.model.enums.RentalStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.HashSet;
import java.util.Set;

@Entity
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column
    @NotBlank
    @Size(max = 20)
    private String firstName;

    @Column
    @NotBlank
    @Size(max = 20)
    private String lastName;

    @Column(nullable = false, unique = true)
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[0-9. ()-]{7,25}$", message = "Invalid phone number format")
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RentalStatus rentalStatus;

    /* Tenant-Listing relationship */
    @OneToOne(mappedBy = "tenant", cascade = {
            CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH
    })
    private Listing listing;

    /* Tenant applications relationship */
    @ManyToMany(cascade = {
            CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH
    })
    @JoinTable(
            name = "tenant_listing_applications",
            joinColumns = @JoinColumn(name = "tenant_id"),
            inverseJoinColumns = @JoinColumn(name = "listing_id")
    )
    private Set<Listing> appliedListings = new HashSet<>();

    /* Tenant-User relationship */
    @OneToOne(cascade = {
            CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH, CascadeType.DETACH
    })
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    public Tenant(String firstName, String lastName, String phoneNumber) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
    }

    public Tenant() {
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Integer getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public Listing getListing() {
        return listing;
    }

    public void setListing(Listing listing) {
        this.listing = listing;
    }

    public RentalStatus getRentalStatus() {
        return rentalStatus;
    }

    public void setRentalStatus(RentalStatus rentalStatus) {
        this.rentalStatus = rentalStatus;
    }

    public Set<Listing> getAppliedListings() {
        return appliedListings;
    }

    public void setAppliedListings(Set<Listing> appliedListings) {
        this.appliedListings = appliedListings;
    }

    public void applyToListing(Listing listing) {

        if (!appliedListings.contains(listing)) {
            appliedListings.add(listing);
            listing.getApplicants().add(this); // bidirectional relationship
        }
    }

    /**
     * Domain Logic: A tenant successfully rents a listing.
     */
    public void rent(Listing listing) {

        if (this.listing != null) {
            throw new IllegalStateException("Tenant is already renting another listing.");
        }

        this.appliedListings.remove(listing); // the winner is no longer an applicant
        this.listing = listing;
        this.rentalStatus = RentalStatus.RENTING;
    }

    /**
     * Domain Logic: A tenant processes a rejection.
     * If they have no other pending applications or active rentals, their status changes.
     */
    public void processRejection(Listing listing) {

        this.appliedListings.remove(listing);

        if (this.appliedListings.isEmpty() && this.listing == null) {
            this.rentalStatus = RentalStatus.CANCELED;
        }
    }


    @Override
    public String toString() {
        return "Tenant{" +
                "id=" + id +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                '}';
    }
}
