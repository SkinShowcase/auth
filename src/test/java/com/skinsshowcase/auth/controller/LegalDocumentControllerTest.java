package com.skinsshowcase.auth.controller;

import com.skinsshowcase.auth.dto.LegalDocumentListItemDto;
import com.skinsshowcase.auth.dto.LegalDocumentResponseDto;
import com.skinsshowcase.auth.service.LegalDocumentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = LegalDocumentController.class)
@AutoConfigureMockMvc(addFilters = false)
class LegalDocumentControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    LegalDocumentService legalDocumentService;

    @Test
    void list_returnsJson() throws Exception {
        when(legalDocumentService.listLatestMeta()).thenReturn(List.of(
                new LegalDocumentListItemDto("tos", 1, "T", Instant.EPOCH)));
        mockMvc.perform(get("/auth/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("tos"));
    }

    @Test
    void getBySlug_returns404WhenEmpty() throws Exception {
        when(legalDocumentService.getBySlug("x", null)).thenReturn(Optional.empty());
        mockMvc.perform(get("/auth/documents/x"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getBySlug_returnsBody() throws Exception {
        when(legalDocumentService.getBySlug("tos", 2)).thenReturn(Optional.of(
                new LegalDocumentResponseDto("tos", 2, "T", "body", Instant.EPOCH)));
        mockMvc.perform(get("/auth/documents/tos").param("version", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("body"));
    }
}
