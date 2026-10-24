package gr.hua.dit.dras.repositories;

/* imports */
import gr.hua.dit.dras.dto.ListingFilterDTO;
import gr.hua.dit.dras.entities.Listing;
import gr.hua.dit.dras.model.enums.ListingStatus;
import gr.hua.dit.dras.model.enums.PropertyType;
import gr.hua.dit.dras.model.enums.RentalDuration;
import gr.hua.dit.dras.services.domain.ListingService;
import gr.hua.dit.dras.services.domain.OwnerService;
import gr.hua.dit.dras.services.domain.TenantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DataJpaTest
class ListingFilterSpecificationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ListingRepository listingRepository;

    private ListingService listingService;

    private Listing listingPanormouStudio;
    private Listing listingGlyfadaPenthouse;
    private Listing listingKifisiaMaisonette;
    private Listing listingKypseliFlat;
    private Listing listingPendingApartment;
    private Listing listingRejectedMaisonette;

    @BeforeEach
    void setUp() {
        listingService = new ListingService(listingRepository, mock(OwnerService.class), mock(TenantService.class));

        // Panormou Studio
        listingPanormouStudio = createListing(
                "Modern Studio in Panormou",
                "Ambelokipi Metro Station",
                "Fully furnished studio close to blue line metro station with central heating",
                450, 30, PropertyType.STUDIO,
                "Panormou 15, Athens 11524",
                1, 1, 2005, false, ListingStatus.APPROVED
        );

        // Glyfada Penthouse
        listingGlyfadaPenthouse = createListing(
                "Luxury Penthouse in Glyfada",
                "Sea View Riviera",
                "Modern high-floor apartment with fireplace, large veranda, and underground parking",
                1500, 125, PropertyType.APARTMENT,
                "Poseidonos 22, Glyfada 16674",
                3, 2, 2018, true, ListingStatus.APPROVED
        );

        // Kifisia Maisonette
        listingKifisiaMaisonette = createListing(
                "Traditional Maisonette in Kifisia",
                "Quiet Green Suburb",
                "Detached family maisonette with private garden, barbecue, and autonomous heating",
                1800, 180, PropertyType.MAISONETTE,
                "Kolokotroni 5, Kifisia 14562",
                4, 3, 2010, false, ListingStatus.APPROVED
        );

        // Kypseli Flat
        listingKypseliFlat = createListing(
                "Economical Flat in Kypseli",
                "Fokionos Negri Pedestrian Area",
                "Cozy student apartment near bus lines and supermarket",
                350, 45, PropertyType.APARTMENT,
                "Drosopoulou 80, Athens 11257",
                1, 1, 1970, true, ListingStatus.APPROVED
        );

        // Pending Apartment in Athens Center
        listingPendingApartment = createListing(
                "Pending Center Apartment",
                "Syntagma Square",
                "Under review apartment in the heart of Athens",
                450, 50, PropertyType.APARTMENT,
                "Solonos 10, Athens 10673",
                1, 1, 2000, false, ListingStatus.PENDING
        );

        // Rejected Maisonette in Glyfada
        listingRejectedMaisonette = createListing(
                "Rejected Luxury Villa",
                "Sea View",
                "Rejected listing containing inappropriate photos",
                1400, 150, PropertyType.MAISONETTE,
                "Poseidonos 99, Glyfada 16675",
                3, 2, 2020, false, ListingStatus.REJECTED
        );

        entityManager.flush();
        entityManager.clear();
    }

    private Listing createListing(String title, String subtitle, String description,
                                  int price, int sizeM2, PropertyType type,
                                  String address, int bedrooms, int bathrooms,
                                  int yearBuilt, boolean external, ListingStatus status) {
        Listing listing = new Listing();
        listing.setTitle(title);
        listing.setSubtitle(subtitle);
        listing.setDescription(description);
        listing.setPrice(price);
        listing.setPricePerM2(Math.max(1, price / (sizeM2 > 0 ? sizeM2 : 1)));
        listing.setSizeM2(sizeM2);
        listing.setPropertyType(type);
        listing.setRentalDuration(RentalDuration.LONG_TERM);
        listing.setAddress(address);
        listing.setBedrooms(bedrooms);
        listing.setBathrooms(bathrooms);
        listing.setYearBuilt(yearBuilt);
        listing.setExternal(external);
        listing.setStatus(status);
        if (external) {
            listing.setSourceUrl("https://example.com/item/" + Math.abs((title + address).hashCode()));
        }
        return entityManager.persistAndFlush(listing);
    }

    private void setUpdatedAtNative(Integer listingId, Instant instant) {
        entityManager.getEntityManager()
                .createNativeQuery("UPDATE listings SET updated_at = :dt WHERE id = :id")
                .setParameter("dt", instant)
                .setParameter("id", listingId)
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();
    }

    /* Status Invariant & Default Filter */

    @Test
    @DisplayName("Empty filter returns only all APPROVED listings, excluding PENDING and REJECTED")
    void filter_DefaultFilter_ReturnsAllApprovedOnly() {
        ListingFilterDTO filter = new ListingFilterDTO();

        List<Listing> result = listingService.filterListings(filter);

        assertThat(result)
                .extracting(Listing::getId)
                .containsExactlyInAnyOrder(
                        listingPanormouStudio.getId(),
                        listingGlyfadaPenthouse.getId(),
                        listingKifisiaMaisonette.getId(),
                        listingKypseliFlat.getId()
                );
    }

    @Test
    @DisplayName("Pending and Rejected listings are never returned even when search matches their fields")
    void filter_UnapprovedListings_NeverReturned() {
        ListingFilterDTO filter = new ListingFilterDTO();
        filter.setSearch("Solonos"); // matches listingPendingApartment

        List<Listing> result = listingService.filterListings(filter);

        assertThat(result).isEmpty();

        filter.setSearch("Inappropriate"); // matches listingRejectedMaisonette
        result = listingService.filterListings(filter);

        assertThat(result).isEmpty();
    }

    /* Keyword Search */

    @Test
    @DisplayName("Search keyword matches title case-insensitively")
    void filter_Search_MatchesTitleCaseInsensitively() {
        ListingFilterDTO filter = new ListingFilterDTO();
        filter.setSearch("pEnThOuSe");

        List<Listing> result = listingService.filterListings(filter);

        assertThat(result)
                .extracting(Listing::getId)
                .containsExactly(listingGlyfadaPenthouse.getId());
    }

    @Test
    @DisplayName("Search keyword matches subtitle case-insensitively")
    void filter_Search_MatchesSubtitleCaseInsensitively() {
        ListingFilterDTO filter = new ListingFilterDTO();
        filter.setSearch("aMbElOkIpI");

        List<Listing> result = listingService.filterListings(filter);

        assertThat(result)
                .extracting(Listing::getId)
                .containsExactly(listingPanormouStudio.getId());
    }

    @Test
    @DisplayName("Search keyword matches address case-insensitively")
    void filter_Search_MatchesAddressCaseInsensitively() {
        ListingFilterDTO filter = new ListingFilterDTO();
        filter.setSearch("drosopoulou");

        List<Listing> result = listingService.filterListings(filter);

        assertThat(result)
                .extracting(Listing::getId)
                .containsExactly(listingKypseliFlat.getId());
    }

    @Test
    @DisplayName("Search keyword matches description case-insensitively")
    void filter_Search_MatchesDescriptionCaseInsensitively() {
        ListingFilterDTO filter = new ListingFilterDTO();
        filter.setSearch("GaRdEn");

        List<Listing> result = listingService.filterListings(filter);

        assertThat(result)
                .extracting(Listing::getId)
                .containsExactly(listingKifisiaMaisonette.getId());
    }

    @Test
    @DisplayName("Multi-token search requires ALL tokens to match across searchable fields")
    void filter_Search_MultiTokenRequiresAllTokensToMatch() {
        ListingFilterDTO filter = new ListingFilterDTO();
        // "Studio" is in title, "Metro" is in subtitle
        filter.setSearch("Studio Metro");

        List<Listing> result = listingService.filterListings(filter);

        assertThat(result)
                .extracting(Listing::getId)
                .containsExactly(listingPanormouStudio.getId());

        // "Studio" matches Panormou, but "Glyfada" does not match Panormou
        filter.setSearch("Studio Glyfada");
        result = listingService.filterListings(filter);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Search keyword with leading/trailing and redundant whitespace is properly tokenized")
    void filter_Search_HandlesWhitespaceCorrectly() {
        ListingFilterDTO filter = new ListingFilterDTO();
        filter.setSearch("   Panormou     Studio   ");

        List<Listing> result = listingService.filterListings(filter);

        assertThat(result)
                .extracting(Listing::getId)
                .containsExactly(listingPanormouStudio.getId());
    }

    @Test
    @DisplayName("Search keyword with no match returns empty list")
    void filter_Search_NoMatchReturnsEmpty() {
        ListingFilterDTO filter = new ListingFilterDTO();
        filter.setSearch("NonExistentKeywordXYZ");

        List<Listing> result = listingService.filterListings(filter);

        assertThat(result).isEmpty();
    }

    /* Title Fallback Search (When 'search' is not provided) */

    @Test
    @DisplayName("Title filter matches title, subtitle, or address when search param is null")
    void filter_TitleFallback_WhenSearchNull() {
        ListingFilterDTO filter = new ListingFilterDTO();
        filter.setTitle("Maisonette");

        List<Listing> result = listingService.filterListings(filter);

        assertThat(result)
                .extracting(Listing::getId)
                .containsExactly(listingKifisiaMaisonette.getId());
    }

    @Test
    @DisplayName("Title filter matches when search param is blank")
    void filter_TitleFallback_WhenSearchBlank() {
        ListingFilterDTO filter = new ListingFilterDTO();
        filter.setSearch("   ");
        filter.setTitle("Riviera");

        List<Listing> result = listingService.filterListings(filter);

        assertThat(result)
                .extracting(Listing::getId)
                .containsExactly(listingGlyfadaPenthouse.getId());
    }

    /* Price Filtering (minPrice, maxPrice, and ranges) */

    @Test
    @DisplayName("minPrice filters listings with price >= minPrice")
    void filter_MinPriceOnly_FiltersLowerBound() {
        ListingFilterDTO filter = new ListingFilterDTO();
        filter.setMinPrice(1500);

        List<Listing> result = listingService.filterListings(filter);

        assertThat(result)
                .extracting(Listing::getId)
                .containsExactlyInAnyOrder(
                        listingGlyfadaPenthouse.getId(),
                        listingKifisiaMaisonette.getId()
                );
    }

    @Test
    @DisplayName("maxPrice filters listings with price <= maxPrice")
    void filter_MaxPriceOnly_FiltersUpperBound() {
        ListingFilterDTO filter = new ListingFilterDTO();
        filter.setMaxPrice(450);

        List<Listing> result = listingService.filterListings(filter);

        assertThat(result)
                .extracting(Listing::getId)
                .containsExactlyInAnyOrder(
                        listingPanormouStudio.getId(),
                        listingKypseliFlat.getId()
                );
    }

    @Test
    @DisplayName("Price range filters listings inclusive of both boundaries")
    void filter_PriceRange_FiltersBothBounds() {
        ListingFilterDTO filter = new ListingFilterDTO();
        filter.setMinPrice(400);
        filter.setMaxPrice(1600);

        List<Listing> result = listingService.filterListings(filter);

        assertThat(result)
                .extracting(Listing::getId)
                .containsExactlyInAnyOrder(
                        listingPanormouStudio.getId(),
                        listingGlyfadaPenthouse.getId()
                );
    }

    /* Property Type Matching */

    @Test
    @DisplayName("PropertyType filters exact enum match")
    void filter_PropertyType_MatchesExactEnum() {
        ListingFilterDTO filter = new ListingFilterDTO();
        filter.setType(PropertyType.STUDIO);

        List<Listing> result = listingService.filterListings(filter);

        assertThat(result)
                .extracting(Listing::getId)
                .containsExactly(listingPanormouStudio.getId());

        filter.setType(PropertyType.APARTMENT);
        result = listingService.filterListings(filter);

        assertThat(result)
                .extracting(Listing::getId)
                .containsExactlyInAnyOrder(
                        listingGlyfadaPenthouse.getId(),
                        listingKypseliFlat.getId()
                );
    }

    /* Municipality and District Filtering */

    @Test
    @DisplayName("Municipality filters address case-insensitively")
    void filter_Municipality_FiltersAddress() {
        ListingFilterDTO filter = new ListingFilterDTO();
        filter.setMunicipality("athens");

        List<Listing> result = listingService.filterListings(filter);

        assertThat(result)
                .extracting(Listing::getId)
                .containsExactlyInAnyOrder(
                        listingPanormouStudio.getId(),
                        listingKypseliFlat.getId()
                );

        filter.setMunicipality("kifisia");
        result = listingService.filterListings(filter);

        assertThat(result)
                .extracting(Listing::getId)
                .containsExactly(listingKifisiaMaisonette.getId());
    }

    @Test
    @DisplayName("District filters address or subtitle case-insensitively")
    void filter_District_FiltersAddressOrSubtitle() {
        ListingFilterDTO filter = new ListingFilterDTO();
        // "Fokionos Negri" is in subtitle of Kypseli flat
        filter.setDistrict("fokionos");

        List<Listing> result = listingService.filterListings(filter);

        assertThat(result)
                .extracting(Listing::getId)
                .containsExactly(listingKypseliFlat.getId());

        // "Poseidonos" is in address of Glyfada penthouse
        filter.setDistrict("poseidonos");
        result = listingService.filterListings(filter);

        assertThat(result)
                .extracting(Listing::getId)
                .containsExactly(listingGlyfadaPenthouse.getId());
    }

    /* Bedrooms, Bathrooms, and Year Built Ranges */

    @Test
    @DisplayName("Bedrooms range filters listings inclusive of bounds")
    void filter_BedroomsRange_FiltersCorrectly() {
        ListingFilterDTO filter = new ListingFilterDTO();
        filter.setMinBedrooms(3);
        filter.setMaxBedrooms(4);

        List<Listing> result = listingService.filterListings(filter);

        assertThat(result)
                .extracting(Listing::getId)
                .containsExactlyInAnyOrder(
                        listingGlyfadaPenthouse.getId(),
                        listingKifisiaMaisonette.getId()
                );
    }

    @Test
    @DisplayName("Bathrooms range filters listings inclusive of bounds")
    void filter_BathroomsRange_FiltersCorrectly() {
        ListingFilterDTO filter = new ListingFilterDTO();
        filter.setMinBathrooms(2);
        filter.setMaxBathrooms(3);

        List<Listing> result = listingService.filterListings(filter);

        assertThat(result)
                .extracting(Listing::getId)
                .containsExactlyInAnyOrder(
                        listingGlyfadaPenthouse.getId(),
                        listingKifisiaMaisonette.getId()
                );
    }

    @Test
    @DisplayName("YearBuilt range filters listings inclusive of bounds")
    void filter_YearBuiltRange_FiltersCorrectly() {
        ListingFilterDTO filter = new ListingFilterDTO();
        filter.setMinYear(2000);
        filter.setMaxYear(2015);

        List<Listing> result = listingService.filterListings(filter);

        assertThat(result)
                .extracting(Listing::getId)
                .containsExactlyInAnyOrder(
                        listingPanormouStudio.getId(),
                        listingKifisiaMaisonette.getId()
                );
    }

    /* External vs Local Origin Filtering */

    @Test
    @DisplayName("externalOnly = true returns only scraped listings")
    void filter_ExternalOnlyTrue_ReturnsScrapedOnly() {
        ListingFilterDTO filter = new ListingFilterDTO();
        filter.setExternalOnly(true);

        List<Listing> result = listingService.filterListings(filter);

        assertThat(result)
                .extracting(Listing::getId)
                .containsExactlyInAnyOrder(
                        listingGlyfadaPenthouse.getId(),
                        listingKypseliFlat.getId()
                );
    }

    @Test
    @DisplayName("externalOnly = false returns only direct / platform listings")
    void filter_ExternalOnlyFalse_ReturnsDirectOnly() {
        ListingFilterDTO filter = new ListingFilterDTO();
        filter.setExternalOnly(false);

        List<Listing> result = listingService.filterListings(filter);

        assertThat(result)
                .extracting(Listing::getId)
                .containsExactlyInAnyOrder(
                        listingPanormouStudio.getId(),
                        listingKifisiaMaisonette.getId()
                );
    }

    /* Updated At Date Filtering */

    @Test
    @DisplayName("updatedAfter and updatedBefore filter listings by updatedAt timestamp")
    void filter_UpdatedDates_FiltersRangeCorrectly() {
        Instant now = Instant.now();
        // Set historical dates
        setUpdatedAtNative(listingKypseliFlat.getId(), now.minus(30, ChronoUnit.DAYS));
        setUpdatedAtNative(listingKifisiaMaisonette.getId(), now.minus(15, ChronoUnit.DAYS));
        setUpdatedAtNative(listingPanormouStudio.getId(), now.minus(5, ChronoUnit.DAYS));
        setUpdatedAtNative(listingGlyfadaPenthouse.getId(), now.minus(1, ChronoUnit.DAYS));

        // Filter updated between 20 days ago and 10 days ago
        ListingFilterDTO filter = new ListingFilterDTO();
        filter.setUpdatedAfter(LocalDate.now().minusDays(20));
        filter.setUpdatedBefore(LocalDate.now().minusDays(10));

        List<Listing> result = listingService.filterListings(filter);

        assertThat(result)
                .extracting(Listing::getId)
                .containsExactly(listingKifisiaMaisonette.getId());

        // Filter updated in the last 7 days
        filter = new ListingFilterDTO();
        filter.setUpdatedAfter(LocalDate.now().minusDays(7));
        filter.setUpdatedBefore(LocalDate.now());

        result = listingService.filterListings(filter);

        assertThat(result)
                .extracting(Listing::getId)
                .containsExactlyInAnyOrder(
                        listingPanormouStudio.getId(),
                        listingGlyfadaPenthouse.getId()
                );
    }

    /* Multi-Criteria Combined Filter */

    @Test
    @DisplayName("Combined filter with search, price, property type, bedrooms, and origin finds exact match")
    void filter_CombinedMultiCriteria_FindsExactListing() {
        ListingFilterDTO filter = new ListingFilterDTO();
        filter.setSearch("Studio");
        filter.setMinPrice(400);
        filter.setMaxPrice(600);
        filter.setType(PropertyType.STUDIO);
        filter.setMunicipality("Athens");
        filter.setMinBedrooms(1);
        filter.setMaxBedrooms(1);
        filter.setMinBathrooms(1);
        filter.setMaxBathrooms(1);
        filter.setMinYear(2000);
        filter.setMaxYear(2010);
        filter.setExternalOnly(false);

        List<Listing> result = listingService.filterListings(filter);

        assertThat(result)
                .extracting(Listing::getId)
                .containsExactly(listingPanormouStudio.getId());
    }

    @Test
    @DisplayName("Combined filter with contradictory criteria returns empty list")
    void filter_CombinedMultiCriteria_ContradictoryReturnsEmpty() {
        ListingFilterDTO filter = new ListingFilterDTO();
        filter.setSearch("Studio");
        filter.setType(PropertyType.APARTMENT); // Contradicts studio

        List<Listing> result = listingService.filterListings(filter);

        assertThat(result).isEmpty();
    }
}
