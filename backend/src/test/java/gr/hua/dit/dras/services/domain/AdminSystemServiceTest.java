package gr.hua.dit.dras.services.domain;

import gr.hua.dit.dras.dto.MacroForecastDTO;
import gr.hua.dit.dras.dto.SystemStatusDTO;
import gr.hua.dit.dras.model.enums.ListingStatus;
import gr.hua.dit.dras.repositories.ListingRepository;
import gr.hua.dit.dras.repositories.OwnerRepository;
import gr.hua.dit.dras.repositories.TenantRepository;
import gr.hua.dit.dras.repositories.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminSystemServiceTest {

    @Mock
    private MacroAnalyticsService macroAnalyticsService;

    @Mock
    private PriceSuggestionService priceSuggestionService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OwnerRepository ownerRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private ListingRepository listingRepository;

    @InjectMocks
    private AdminSystemService adminSystemService;

    @Test
    @DisplayName("getSystemStatus populates all platform statistics and listing origin breakdowns")
    void testGetSystemStatus_Success() {
        when(userRepository.count()).thenReturn(10L);
        when(ownerRepository.count()).thenReturn(4L);
        when(tenantRepository.count()).thenReturn(5L);

        when(listingRepository.count()).thenReturn(12L);
        when(listingRepository.countByExternalFalse()).thenReturn(5L);
        when(listingRepository.countByExternalTrue()).thenReturn(7L);

        when(listingRepository.countByStatus(ListingStatus.APPROVED)).thenReturn(8L);
        when(listingRepository.countByStatus(ListingStatus.PENDING)).thenReturn(2L);
        when(listingRepository.countByStatus(ListingStatus.RENTED)).thenReturn(1L);
        when(listingRepository.countByStatus(ListingStatus.REJECTED)).thenReturn(1L);

        when(macroAnalyticsService.getMacroForecast()).thenReturn(new MacroForecastDTO());

        SystemStatusDTO status = adminSystemService.getSystemStatus();

        assertThat(status).isNotNull();
        assertThat(status.getTotalUsers()).isEqualTo(10L);
        assertThat(status.getTotalOwners()).isEqualTo(4L);
        assertThat(status.getTotalTenants()).isEqualTo(5L);
        assertThat(status.getTotalListings()).isEqualTo(12L);
        assertThat(status.getLocalListings()).isEqualTo(5L);
        assertThat(status.getExternalListings()).isEqualTo(7L);
        assertThat(status.getActiveListings()).isEqualTo(8L);
        assertThat(status.getPendingListings()).isEqualTo(2L);
        assertThat(status.getRentedListings()).isEqualTo(1L);
        assertThat(status.getRejectedListings()).isEqualTo(1L);
        assertThat(status.getLastReloadedAge()).isEqualTo("just now");
        assertThat(status.isMacroPredictionsLoaded()).isTrue();
    }

    @Test
    @DisplayName("getSystemStatus handles uninitialized macro forecast gracefully")
    void testGetSystemStatus_UninitializedForecast() {
        when(userRepository.count()).thenReturn(0L);
        when(ownerRepository.count()).thenReturn(0L);
        when(tenantRepository.count()).thenReturn(0L);
        when(listingRepository.count()).thenReturn(0L);
        when(listingRepository.countByExternalFalse()).thenReturn(0L);
        when(listingRepository.countByExternalTrue()).thenReturn(0L);
        when(listingRepository.countByStatus(any())).thenReturn(0L);

        when(macroAnalyticsService.getMacroForecast()).thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));

        SystemStatusDTO status = adminSystemService.getSystemStatus();

        assertThat(status).isNotNull();
        assertThat(status.isMacroPredictionsLoaded()).isFalse();
    }

    @Test
    @DisplayName("reloadForecastData reloads both macro and price model data")
    void testReloadForecastData() {
        adminSystemService.reloadForecastData();

        verify(macroAnalyticsService, times(1)).loadModelData();
        verify(priceSuggestionService, times(1)).loadModelData();
    }
}
