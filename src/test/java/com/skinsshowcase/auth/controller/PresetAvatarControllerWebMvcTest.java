package com.skinsshowcase.auth.controller;

import com.skinsshowcase.auth.dto.PresetAvatarOptionDto;
import com.skinsshowcase.auth.service.PresetAvatarService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PresetAvatarController.class)
@AutoConfigureMockMvc(addFilters = false)
class PresetAvatarControllerWebMvcTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    PresetAvatarService presetAvatarService;

    @Test
    void listPresets() throws Exception {
        when(presetAvatarService.listOptions()).thenReturn(List.of(new PresetAvatarOptionDto(1, "http://x/1")));
        mockMvc.perform(get("/auth/avatars"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void getPresetImage() throws Exception {
        when(presetAvatarService.loadImageBytes(1)).thenReturn(new byte[] { (byte) 0xff, (byte) 0xd8 });
        mockMvc.perform(get("/auth/avatars/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"));
    }
}
