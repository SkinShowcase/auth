package com.skinsshowcase.auth.controller;

import com.skinsshowcase.auth.client.SteamGatewayProfileClient;
import com.skinsshowcase.auth.repository.UserRepository;
import com.skinsshowcase.auth.service.JwtService;
import com.skinsshowcase.auth.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Проверка работоспособности аутентификации: редирект на Steam, /me без токена и с JWT.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class AuthControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JwtService jwtService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    UserService userService;

    @MockBean
    SteamGatewayProfileClient steamGatewayProfileClient;

    @Test
    void session_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/auth/session"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void session_withValidToken_returns204() throws Exception {
        var steamId = "76561198000000002";
        var token = jwtService.createToken(steamId);
        mockMvc.perform(get("/auth/session")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void session_withBlockedUser_returns403() throws Exception {
        var steamId = "76561198000000003";
        userService.getOrCreate(steamId);
        userService.setUserBlocked(steamId, true);
        var token = jwtService.createToken(steamId);
        mockMvc.perform(get("/auth/session")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Account blocked"));
    }

    @Test
    void me_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_withInvalidToken_returns401() throws Exception {
        mockMvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_withValidToken_returns200AndSteamId() throws Exception {
        var steamId = "76561198000000001";
        var token = jwtService.createToken(steamId);

        mockMvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.steamId").value(steamId))
                .andExpect(jsonPath("$.blocked").value(false));
    }

    @Test
    void steam_login_redirectsToSteamOpenId() throws Exception {
        mockMvc.perform(get("/auth/steam"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", containsString("steamcommunity.com/openid")));
    }

    @Test
    void patchTradeLink_valid_returnsOkWithLink() throws Exception {
        var steamId = "76561198000000010";
        var token = jwtService.createToken(steamId);
        var url = "https://steamcommunity.com/tradeoffer/new/?partner=12345&token=abcdef";
        mockMvc.perform(patch("/auth/me/trade-link")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tradeUrl\":\"" + url + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.steamTradeLink").value(url));
    }

    @Test
    void patchTradeLink_invalidUrl_returns400() throws Exception {
        var steamId = "76561198000000011";
        var token = jwtService.createToken(steamId);
        mockMvc.perform(patch("/auth/me/trade-link")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tradeUrl\":\"https://evil.example/phish\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getOtherUserTradeLink_public_returns200() throws Exception {
        var targetId = "76561198000000020";
        userService.getOrCreate(targetId);
        var url = "https://steamcommunity.com/tradeoffer/new/?partner=1&token=xx";
        userService.updateSteamTradeLink(targetId, url);
        var viewerToken = jwtService.createToken("76561198000000021");
        mockMvc.perform(get("/auth/users/" + targetId + "/trade-link")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tradeUrl").value(url));
    }

    @Test
    void getOtherUserTradeLink_private_returns404() throws Exception {
        var targetId = "76561198000000022";
        userService.getOrCreate(targetId);
        userService.setPrivacy(targetId, true);
        userService.updateSteamTradeLink(targetId, "https://steamcommunity.com/tradeoffer/new/?partner=1&token=yy");
        var viewerToken = jwtService.createToken("76561198000000023");
        mockMvc.perform(get("/auth/users/" + targetId + "/trade-link")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void getUserTradeLink_invalidSteamId_returns400() throws Exception {
        var token = jwtService.createToken("76561198000000024");
        mockMvc.perform(get("/auth/users/not-a-steam-id/trade-link")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listPresetAvatars_withoutAuth_returns200AndEightItems() throws Exception {
        mockMvc.perform(get("/auth/avatars"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(8))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].url").value("http://localhost:8080/auth/avatars/1"));
    }

    @Test
    void patchAvatar_presetValid_updatesEffectiveUrl() throws Exception {
        var steamId = "76561198000000030";
        var token = jwtService.createToken(steamId);
        mockMvc.perform(patch("/auth/me/avatar")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"presetAvatarId\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarSource").value("PRESET"))
                .andExpect(jsonPath("$.selectedPresetAvatarId").value(2))
                .andExpect(jsonPath("$.effectiveAvatarUrl").value("http://localhost:8080/auth/avatars/2"));
    }

    @Test
    void patchAvatar_presetInvalidId_returns400() throws Exception {
        var token = jwtService.createToken("76561198000000031");
        mockMvc.perform(patch("/auth/me/avatar")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"presetAvatarId\":99}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void patchAvatar_missingPresetId_returns400() throws Exception {
        var token = jwtService.createToken("76561198000000032");
        mockMvc.perform(patch("/auth/me/avatar")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getByUsername_whenTargetPrivateAndOtherViewer_returns404() throws Exception {
        var targetId = "76561198000000060";
        userService.getOrCreate(targetId);
        userService.updateDisplayName(targetId, "private_showcase_user");
        userService.setPrivacy(targetId, true);
        var viewerToken = jwtService.createToken("76561198000000061");
        mockMvc.perform(get("/auth/users/by-username/private_showcase_user")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void getByUsername_whenTargetPrivateAndSelf_returns200() throws Exception {
        var targetId = "76561198000000062";
        userService.getOrCreate(targetId);
        userService.updateDisplayName(targetId, "private_self_lookup");
        userService.setPrivacy(targetId, true);
        var token = jwtService.createToken(targetId);
        mockMvc.perform(get("/auth/users/by-username/private_self_lookup")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.steamId").value(targetId));
    }

    @Test
    void getByUsername_whenPublic_returns200() throws Exception {
        var targetId = "76561198000000063";
        userService.getOrCreate(targetId);
        userService.updateDisplayName(targetId, "public_showcase_user");
        var viewerToken = jwtService.createToken("76561198000000064");
        mockMvc.perform(get("/auth/users/by-username/public_showcase_user")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.steamId").value(targetId));
    }

    @Test
    void batchPresetAvatarIds_returnsPresetIdForPresetUser() throws Exception {
        var viewerSteamId = "76561198000000050";
        var otherSteamId = "76561198000000051";
        var other = userService.getOrCreate(otherSteamId);
        other.setSelectedPresetAvatarId(3);
        userRepository.save(other);
        var token = jwtService.createToken(viewerSteamId);
        mockMvc.perform(post("/auth/users/preset-avatar-ids")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"steamIds\":[\"" + otherSteamId + "\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.presetAvatarIdBySteamId['" + otherSteamId + "']").value(3));
    }
}
