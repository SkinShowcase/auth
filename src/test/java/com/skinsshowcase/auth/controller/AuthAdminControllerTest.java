package com.skinsshowcase.auth.controller;

import com.skinsshowcase.auth.config.AdminApiProperties;
import com.skinsshowcase.auth.dto.AdminReportsPageDto;
import com.skinsshowcase.auth.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthAdminControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    UserService userService;

    @MockBean
    AdminApiProperties adminApiProperties;

    @BeforeEach
    void adminKey() {
        when(adminApiProperties.getApiKey()).thenReturn("admin-secret");
    }

    @Test
    void listReports_unauthorizedWithoutKey() throws Exception {
        mockMvc.perform(get("/auth/admin/reports")
                        .header("X-Admin-Api-Key", "wrong"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listReports_ok() throws Exception {
        when(userService.listReportsForAdmin(0, 20)).thenReturn(
                new AdminReportsPageDto(List.of(), 0, 20, 0, 0));
        mockMvc.perform(get("/auth/admin/reports")
                        .header("X-Admin-Api-Key", "admin-secret"))
                .andExpect(status().isOk());
    }

    @Test
    void blockUser_ok() throws Exception {
        mockMvc.perform(post("/auth/admin/users/76561198000000001/block")
                        .header("X-Admin-Api-Key", "admin-secret"))
                .andExpect(status().isNoContent());
    }

    @Test
    void unblockUser_ok() throws Exception {
        mockMvc.perform(post("/auth/admin/users/76561198000000001/unblock")
                        .header("X-Admin-Api-Key", "admin-secret"))
                .andExpect(status().isNoContent());
    }
}
