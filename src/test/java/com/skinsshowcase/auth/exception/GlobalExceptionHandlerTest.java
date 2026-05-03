package com.skinsshowcase.auth.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestApi())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void steamAuth_returns401() throws Exception {
        mockMvc.perform(get("/__test/steam-auth"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Steam authentication failed"));
    }

    @Test
    void accountBlocked_returns403() throws Exception {
        mockMvc.perform(get("/__test/blocked"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Account blocked"));
    }

    @Test
    void conflict_returns409WithMessage() throws Exception {
        mockMvc.perform(get("/__test/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("name taken"));
    }

    @Test
    void illegalArgument_returns400() throws Exception {
        mockMvc.perform(get("/__test/bad-arg"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid id"));
    }

    @Test
    void validation_returns400WithFieldDetails() throws Exception {
        var result = mockMvc.perform(post("/__test/valid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andReturn();
        assertThat(result.getResponse().getContentAsString()).contains("name");
    }

    @Test
    void invalidJson_returns400() throws Exception {
        mockMvc.perform(post("/__test/echo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @RestController
    static class TestApi {

        @GetMapping("/__test/steam-auth")
        void steamAuth() {
            throw new SteamAuthException("fail");
        }

        @GetMapping("/__test/blocked")
        void blocked() {
            throw new AccountBlockedException();
        }

        @GetMapping("/__test/conflict")
        void conflict() {
            throw new ConflictException("name taken");
        }

        @GetMapping("/__test/bad-arg")
        void badArg() {
            throw new IllegalArgumentException("invalid id");
        }

        record ValidBody(@NotBlank String name) {
        }

        @PostMapping("/__test/valid")
        void validate(@Valid @RequestBody ValidBody body) {
        }

        record EchoBody(String x) {
        }

        @PostMapping("/__test/echo")
        void echo(@RequestBody EchoBody body) {
        }
    }
}
