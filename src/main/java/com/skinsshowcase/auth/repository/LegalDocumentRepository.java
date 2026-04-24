package com.skinsshowcase.auth.repository;

import com.skinsshowcase.auth.entity.LegalDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface LegalDocumentRepository extends JpaRepository<LegalDocument, Long> {

    @Query("""
            SELECT d FROM LegalDocument d
            WHERE d.slug = :slug
            ORDER BY d.effectiveFrom DESC, d.version DESC
            """)
    List<LegalDocument> findAllBySlugOrderNewest(String slug);

    @Query(value = "SELECT DISTINCT slug FROM legal_document ORDER BY slug", nativeQuery = true)
    List<String> findAllDistinctSlugs();

    @Query("""
            SELECT d FROM LegalDocument d
            WHERE d.slug = :slug AND d.version = :version
            """)
    Optional<LegalDocument> findBySlugAndVersion(String slug, int version);
}
