package gr.hua.dit.dras.services.integration;

import gr.hua.dit.dras.entities.Listing;
import gr.hua.dit.dras.repositories.ListingRepository;
import gr.hua.dit.dras.services.domain.ListingImagePolicy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductionListingImageCleanupTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Startup cleanup drops remote and scraped paths but keeps local assets")
    void run_ClearsDisallowedImages() {
        ListingRepository repository = mock(ListingRepository.class);
        ListingImagePolicy policy = new ListingImagePolicy(false, tempDir.toString());

        Listing external = new Listing();
        external.setExternal(true);
        external.setImages(List.of("https://cdn.example.com/photo.jpg"));

        Listing local = new Listing();
        local.setExternal(false);
        local.setImages(List.of(
                "https://cdn.example.com/photo.jpg",
                "data/images/1/a.jpg",
                "/images/demo/image1.jpg"
        ));

        Listing clean = new Listing();
        clean.setExternal(false);
        clean.setImages(List.of("/images/abc.jpg"));

        when(repository.findAll(any(Pageable.class))).thenReturn(
                new PageImpl<>(List.of(external, local, clean), PageRequest.of(0, 500), 3));

        new ProductionListingImageCleanup(repository, policy).run(new DefaultApplicationArguments(new String[0]));

        ArgumentCaptor<Iterable<Listing>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(repository).saveAll(captor.capture());
        List<Listing> saved = (List<Listing>) captor.getValue();
        assertThat(saved).containsExactly(external, local);
        assertThat(external.getImages()).isEmpty();
        assertThat(local.getImages()).containsExactly("/images/demo/image1.jpg");
        assertThat(clean.getImages()).containsExactly("/images/abc.jpg");
    }

    @Test
    @DisplayName("Startup cleanup skips the database write when nothing changes")
    void run_DoesNothingWhenAlreadyClean() {
        ListingRepository repository = mock(ListingRepository.class);
        ListingImagePolicy policy = new ListingImagePolicy(false, tempDir.toString());

        Listing listing = new Listing();
        listing.setExternal(false);
        listing.setImages(List.of("images/demo/image1.jpg"));
        when(repository.findAll(any(Pageable.class))).thenReturn(
                new PageImpl<>(List.of(listing), PageRequest.of(0, 500), 1));

        new ProductionListingImageCleanup(repository, policy).run(new DefaultApplicationArguments(new String[0]));

        verify(repository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Startup cleanup processes every page")
    void run_ProcessesAllPages() {
        ListingRepository repository = mock(ListingRepository.class);
        ListingImagePolicy policy = new ListingImagePolicy(false, tempDir.toString());

        Listing first = new Listing();
        first.setExternal(true);
        first.setImages(List.of("https://cdn.example.com/one.jpg"));

        Listing second = new Listing();
        second.setExternal(true);
        second.setImages(List.of("https://cdn.example.com/two.jpg"));

        PageImpl<Listing> firstPage = new PageImpl<>(List.of(first), PageRequest.of(0, 500), 1000);
        PageImpl<Listing> secondPage = new PageImpl<>(List.of(second), PageRequest.of(1, 500), 1000);
        when(repository.findAll(any(Pageable.class))).thenAnswer(invocation -> {
            Pageable pageable = invocation.getArgument(0);
            return pageable.getPageNumber() == 0 ? firstPage : secondPage;
        });

        new ProductionListingImageCleanup(repository, policy).run(new DefaultApplicationArguments(new String[0]));

        verify(repository, times(2)).saveAll(any());
        assertThat(first.getImages()).isEmpty();
        assertThat(second.getImages()).isEmpty();
    }
}
