package dev.cylox.msm.profile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public final class SessionProfileStore {
    private final Path file;

    public SessionProfileStore(Path file) {
        if (file == null) throw new IllegalArgumentException("Profile file is required");
        this.file = file;
    }

    public List<SessionProfile> load() throws IOException {
        InputStream input;
        try {
            input = Files.newInputStream(file);
        } catch (NoSuchFileException exception) {
            if (Files.isSymbolicLink(file)) throw exception;
            return Collections.emptyList();
        }

        Properties properties = new Properties();
        try (InputStream opened = input) {
            try { properties.load(opened); }
            catch (IllegalArgumentException exception) { throw new IOException("Invalid profile properties", exception); }
        }

        String storedCount = properties.getProperty("profile.count");
        if (storedCount == null) throw new IOException("Missing profile count");
        int count;
        try { count = Integer.parseInt(storedCount); }
        catch (NumberFormatException exception) { throw new IOException("Invalid profile count", exception); }
        if (count < 0) throw new IOException("Profile count is out of range");
        if (count > properties.size() / 2) throw new IOException("Profile count does not match the stored profile entries");

        List<SessionProfile> profiles = new ArrayList<SessionProfile>();
        for (int index = 0; index < count; index++) {
            String prefix = "profile." + index + ".";
            try {
                profiles.add(new SessionProfile(properties.getProperty(prefix + "username"),
                        properties.getProperty(prefix + "server"),
                        SessionAuthMode.fromStoredValue(properties.getProperty(prefix + "authMode"))));
            } catch (IllegalArgumentException exception) {
                throw new IOException("Invalid saved profile at index " + index, exception);
            }
        }
        return profiles;
    }

    public void save(List<SessionProfile> profiles) throws IOException {
        if (profiles == null) throw new IllegalArgumentException("Profile list is required");
        Properties properties = new Properties();
        properties.setProperty("profile.count", Integer.toString(profiles.size()));
        for (int index = 0; index < profiles.size(); index++) {
            SessionProfile profile = profiles.get(index);
            if (profile == null) throw new IllegalArgumentException("Profile cannot be null");
            String prefix = "profile." + index + ".";
            properties.setProperty(prefix + "username", profile.getUsername());
            properties.setProperty(prefix + "server", profile.getServerAddress());
            properties.setProperty(prefix + "authMode", profile.getAuthMode().name());
        }

        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, "msm-profiles-", ".tmp");
        try {
            try (OutputStream output = Files.newOutputStream(temporary)) { properties.store(output, "MSM profiles; credentials are not stored"); }
            try { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException exception) { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }
}
