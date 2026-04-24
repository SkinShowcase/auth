package com.skinsshowcase.auth.controller;

import com.skinsshowcase.auth.dto.LegalDocumentListItemDto;
import com.skinsshowcase.auth.dto.LegalDocumentResponseDto;
import com.skinsshowcase.auth.service.LegalDocumentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Публичные юридические документы (без JWT).
 */
@RestController
@RequestMapping("/auth/documents")
public class LegalDocumentController {

    private final LegalDocumentService legalDocumentService;

    public LegalDocumentController(LegalDocumentService legalDocumentService) {
        this.legalDocumentService = legalDocumentService;
    }

    @GetMapping
    public List<LegalDocumentListItemDto> list() {
        return legalDocumentService.listLatestMeta();
    }

    @GetMapping("/{slug}")
    public ResponseEntity<LegalDocumentResponseDto> getBySlug(
            @PathVariable String slug,
            @RequestParam(required = false) Integer version) {
        return ResponseEntity.of(legalDocumentService.getBySlug(slug, version));
    }
}
