package dev.cylox.msm.profile;

import java.util.Objects;
import java.util.regex.Pattern;

/** A saved Minecraft identity that may exist before any server is configured. */
public final class AccountIdentity {
    private static final Pattern USERNAME_PATTERN = Pattern.compile("[A-Za-z0-9_]{1,16}");

    private final String username;
    private final SessionAuthMode authMode;

    public AccountIdentity(String username, SessionAuthMode authMode) {
        String normalizedUsername = username == null ? "" : username.trim();
        if (!USERNAME_PATTERN.matcher(normalizedUsername).matches()) {
            throw new IllegalArgumentException("Username must contain 1-16 letters, numbers, or underscores");
        }
        if (authMode == null) throw new IllegalArgumentException("Account authentication mode is required");
        this.username = normalizedUsername;
        this.authMode = authMode;
    }

    public String getUsername() { return username; }
    public SessionAuthMode getAuthMode() { return authMode; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof AccountIdentity)) return false;
        AccountIdentity that = (AccountIdentity) other;
        return username.equals(that.username) && authMode == that.authMode;
    }

    @Override
    public int hashCode() { return Objects.hash(username, authMode); }
}
