package com.skinsshowcase.auth.controller;

import com.skinsshowcase.auth.config.AuthInternalProperties;
import com.skinsshowcase.auth.entity.User;
import com.skinsshowcase.auth.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthInternalUserController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthInternalUserControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    UserService userService;

    @MockBean
    AuthInternalProperties internalProperties;

    @BeforeEach
    void internalKey() {
        when(internalProperties.getServiceKey()).thenReturn("svc-key");
    }

    @Test
    void getPrivacy_badSteam_returns400() throws Exception {
        when(userService.matchesSteamIdFormat("x")).thenReturn(false);
        mockMvc.perform(get("/auth/internal/users/x/privacy")
                        .header("X-Internal-Service-Key", "svc-key"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getPrivacy_notFound() throws Exception {
        when(userService.matchesSteamIdFormat("76561198000000001")).thenReturn(true);
        when(userService.getBySteamId("76561198000000001")).thenReturn(Optional.empty());
        mockMvc.perform(get("/auth/internal/users/76561198000000001/privacy")
                        .header("X-Internal-Service-Key", "svc-key"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getPrivacy_ok() throws Exception {
        when(userService.matchesSteamIdFormat("76561198000000001")).thenReturn(true);
        var u = new User("h");
        u.setPrivateProfile(true);
        when(userService.getBySteamId("76561198000000001")).thenReturn(Optional.of(u));
        mockMvc.perform(get("/auth/internal/users/76561198000000001/privacy")
                        .header("X-Internal-Service-Key", "svc-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.private").value(true));
    }

    @Test
    void privacyFlags_wrongKey() throws Exception {
        mockMvc.perform(post("/auth/internal/users/privacy-flags")
                        .header("X-Internal-Service-Key", "bad")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"steamIds\":[\"76561198000000001\"]}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void privacyFlags_ok() throws Exception {
        when(userService.mapPrivateProfileBySteamId(anyCollection()))
                .thenReturn(Map.of("76561198000000001", false));
        mockMvc.perform(post("/auth/internal/users/privacy-flags")
                        .header("X-Internal-Service-Key", "svc-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"steamIds\":[\"76561198000000001\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.privateBySteamId['76561198000000001']").value(false));
    }

    @Test
    void profileLabels_ok() throws Exception {
        when(userService.mapProfileLabelBySteamId(anyCollection()))
                .thenReturn(Map.of("76561198000000001", "Nick"));
        mockMvc.perform(post("/auth/internal/users/profile-labels")
                        .header("X-Internal-Service-Key", "svc-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"steamIds\":[\"76561198000000001\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.labelBySteamId['76561198000000001']").value("Nick"));
    }
}
