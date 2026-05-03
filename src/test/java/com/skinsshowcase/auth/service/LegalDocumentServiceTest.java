package com.skinsshowcase.auth.service;

import com.skinsshowcase.auth.entity.LegalDocument;
import com.skinsshowcase.auth.repository.LegalDocumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegalDocumentServiceTest {

    @Mock
    private LegalDocumentRepository legalDocumentRepository;

    @InjectMocks
    private LegalDocumentService legalDocumentService;

    @Test
    void listLatestMeta_mapsSlugs() {
        when(legalDocumentRepository.findAllDistinctSlugs()).thenReturn(List.of("tos"));
        var doc = mock(LegalDocument.class);
        when(doc.getSlug()).thenReturn("tos");
        when(doc.getVersion()).thenReturn(2);
        when(doc.getTitle()).thenReturn("TOS");
        when(doc.getEffectiveFrom()).thenReturn(Instant.EPOCH);
        when(legalDocumentRepository.findAllBySlugOrderNewest("tos")).thenReturn(List.of(doc));

        var list = legalDocumentService.listLatestMeta();
        assertThat(list).hasSize(1);
        assertThat(list.get(0).version()).isEqualTo(2);
    }

    @Test
    void getBySlug_blank_empty() {
        assertThat(legalDocumentService.getBySlug("  ", null)).isEmpty();
    }

    @Test
    void getBySlug_withVersion() {
        var doc = mock(LegalDocument.class);
        when(doc.getSlug()).thenReturn("privacy");
        when(doc.getVersion()).thenReturn(1);
        when(doc.getTitle()).thenReturn("P");
        when(doc.getContent()).thenReturn("c");
        when(doc.getEffectiveFrom()).thenReturn(Instant.EPOCH);
        when(legalDocumentRepository.findBySlugAndVersion("privacy", 1)).thenReturn(Optional.of(doc));

        var dto = legalDocumentService.getBySlug("privacy", 1);
        assertThat(dto).isPresent();
        assertThat(dto.get().content()).isEqualTo("c");
    }

    @Test
    void getBySlug_latest() {
        var doc = mock(LegalDocument.class);
        when(doc.getSlug()).thenReturn("privacy");
        when(doc.getVersion()).thenReturn(3);
        when(doc.getTitle()).thenReturn("P");
        when(doc.getContent()).thenReturn("x");
        when(doc.getEffectiveFrom()).thenReturn(Instant.EPOCH);
        when(legalDocumentRepository.findAllBySlugOrderNewest("privacy")).thenReturn(List.of(doc));

        var dto = legalDocumentService.getBySlug("privacy", null);
        assertThat(dto).isPresent();
        assertThat(dto.get().version()).isEqualTo(3);
    }
}
