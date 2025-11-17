package gr.hua.dit.dras.entities;

/* imports */
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
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

    @Column
    @Size(max = 50)
    @Email
    private String email;

    @Column(nullable = false, unique = true)
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[0-9. ()-]{7,25}$", message = "Invalid phone number format")
    private String phoneNumber;

    /* Tenant-Listing relationship */
    @OneToOne(mappedBy = "tenant", cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH})
    private Listing listing;

    /* Tenant applications relationship */
    @ManyToMany(cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH})
    @JoinTable(
            name = "tenant_listing_applications",
            joinColumns = @JoinColumn(name = "tenant_id"),
            inverseJoinColumns = @JoinColumn(name = "listing_id")
    )
    private Set<Listing> appliedListings = new HashSet<>();

    public Set<Listing> getAppliedListings() {
        return appliedListings;
    }

    public void setAppliedListings(Set<Listing> appliedListings) {
        this.appliedListings = appliedListings;
    }

    /* Tenant-User relationship */
    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH, CascadeType.DETACH, CascadeType.REMOVE})
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public Tenant(String firstName, String lastName, String email, String phoneNumber) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phoneNumber = phoneNumber;
    }

    public Tenant() {
    }

    private String username;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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

    public String getEmail() {return email;}

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

    public void setEmail(String email) {this.email = email;}

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public Listing getListing() {
        return listing;
    }

    public void setListing(Listing listing) {
        this.listing = listing;
    }

    public enum RentalStatus {
        APPLIED,
        RENTING,
        CANCELED
    }

    private RentalStatus rentalStatus;

    public RentalStatus getRentalStatus() {
        return rentalStatus;
    }

    public void setRentalStatus(RentalStatus rentalStatus) {
        this.rentalStatus = rentalStatus;
    }

    public void applyToListing(Listing listing) {

        if (!appliedListings.contains(listing)) {
            appliedListings.add(listing);
            listing.getApplicants().add(this); //bidirectional relationship
        }
    }

    @Override
    public String toString() {
        return "Tenant{" +
                "id=" + id +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                '}';
    }
}
