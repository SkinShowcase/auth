package com.skinsshowcase.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

/**
 * Профиль пользователя (Steam). Создаётся при первом входе через Steam OpenID.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @Column(name = "steam_id", length = 64, nullable = false, unique = true)
    private String steamId;

    @Column(name = "private_profile", nullable = false)
    private boolean privateProfile = false;

    @Column(name = "successful_trades_count", nullable = false)
    private int successfulTradesCount = 0;

    @Column(name = "last_online_at")
    private Instant lastOnlineAt;

    /**
     * Ссылка для обмена Steam (https://steamcommunity.com/tradeoffer/new/?partner=...&token=...).
     */
    @Column(name = "steam_trade_link", length = 512)
    private String steamTradeLink;

    @Column(name = "display_name", length = 128, unique = true)
    private String displayName;

    @Column(name = "display_name_hash", length = 64)
    private String displayNameHash;

    @Column(name = "avatar_source", nullable = false, length = 16)
    private String avatarSource = "PRESET";

    @Column(name = "blocked", nullable = false)
    private boolean blocked = false;

    @Column(name = "selected_preset_avatar_id")
    private Integer selectedPresetAvatarId = 1;

    protected User() {
    }

    public User(String steamId) {
        this.steamId = Objects.requireNonNull(steamId, "steamId");
    }

    public String getSteamId() {
        return steamId;
    }

    public boolean isPrivateProfile() {
        return privateProfile;
    }

    public void setPrivateProfile(boolean privateProfile) {
        this.privateProfile = privateProfile;
    }

    public int getSuccessfulTradesCount() {
        return successfulTradesCount;
    }

    public void setSuccessfulTradesCount(int successfulTradesCount) {
        this.successfulTradesCount = Math.max(0, successfulTradesCount);
    }

    public Instant getLastOnlineAt() {
        return lastOnlineAt;
    }

    public void setLastOnlineAt(Instant lastOnlineAt) {
        this.lastOnlineAt = lastOnlineAt;
    }

    public String getSteamTradeLink() {
        return steamTradeLink;
    }

    public void setSteamTradeLink(String steamTradeLink) {
        this.steamTradeLink = steamTradeLink;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayNameHash() {
        return displayNameHash;
    }

    public void setDisplayNameHash(String displayNameHash) {
        this.displayNameHash = displayNameHash;
    }

    public String getAvatarSource() {
        return avatarSource;
    }

    public void setAvatarSource(String avatarSource) {
        this.avatarSource = avatarSource != null ? avatarSource : "PRESET";
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public Integer getSelectedPresetAvatarId() {
        return selectedPresetAvatarId;
    }

    public void setSelectedPresetAvatarId(Integer selectedPresetAvatarId) {
        this.selectedPresetAvatarId = selectedPresetAvatarId;
    }
}
