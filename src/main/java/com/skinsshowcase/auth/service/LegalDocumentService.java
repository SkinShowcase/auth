package com.skinsshowcase.auth.service;

import com.skinsshowcase.auth.dto.LegalDocumentListItemDto;
import com.skinsshowcase.auth.dto.LegalDocumentResponseDto;
import com.skinsshowcase.auth.entity.LegalDocument;
import com.skinsshowcase.auth.repository.LegalDocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class LegalDocumentService {

    private final LegalDocumentRepository legalDocumentRepository;

    public LegalDocumentService(LegalDocumentRepository legalDocumentRepository) {
        this.legalDocumentRepository = legalDocumentRepository;
    }

    @Transactional(readOnly = true)
    public List<LegalDocumentListItemDto> listLatestMeta() {
        var slugs = legalDocumentRepository.findAllDistinctSlugs();
        var out = new ArrayList<LegalDocumentListItemDto>();
        for (var slug : slugs) {
            var latest = findLatestDocument(slug);
            if (latest.isPresent()) {
                out.add(toListItem(latest.get()));
            }
        }
        return out;
    }

    @Transactional(readOnly = true)
    public Optional<LegalDocumentResponseDto> getBySlug(String slug, Integer version) {
        if (slug == null || slug.isBlank()) {
            return Optional.empty();
        }
        var trimmed = slug.trim();
        if (version != null) {
            return legalDocumentRepository.findBySlugAndVersion(trimmed, version).map(this::toResponse);
        }
        return findLatestDocument(trimmed).map(this::toResponse);
    }

    private Optional<LegalDocument> findLatestDocument(String slug) {
        var list = legalDocumentRepository.findAllBySlugOrderNewest(slug);
        if (list.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(list.get(0));
    }

    private LegalDocumentListItemDto toListItem(LegalDocument d) {
        return new LegalDocumentListItemDto(d.getSlug(), d.getVersion(), d.getTitle(), d.getEffectiveFrom());
    }

    private LegalDocumentResponseDto toResponse(LegalDocument d) {
        return new LegalDocumentResponseDto(d.getSlug(), d.getVersion(), d.getTitle(), d.getContent(), d.getEffectiveFrom());
    }
}
