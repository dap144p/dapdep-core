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

/** Persists account identities independently from their server profiles. */
public final class AccountIdentityStore {
    private final Path file;

    public AccountIdentityStore(Path file) {
        if (file == null) throw new IllegalArgumentException("Account file is required");
        this.file = file;
    }

    public List<AccountIdentity> load() throws IOException {
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
            catch (IllegalArgumentException exception) {
                throw new IOException("Invalid account properties", exception);
            }
        }

        String storedCount = properties.getProperty("account.count");
        if (storedCount == null) throw new IOException("Missing account count");
        int count;
        try { count = Integer.parseInt(storedCount); }
        catch (NumberFormatException exception) { throw new IOException("Invalid account count", exception); }
        if (count < 0) throw new IOException("Account count is out of range");
        if (count > properties.size() / 2) {
            throw new IOException("Account count does not match the stored account entries");
        }

        List<AccountIdentity> accounts = new ArrayList<AccountIdentity>();
        for (int index = 0; index < count; index++) {
            String prefix = "account." + index + ".";
            try {
                accounts.add(new AccountIdentity(properties.getProperty(prefix + "username"),
                        SessionAuthMode.fromStoredValue(properties.getProperty(prefix + "authMode"))));
            } catch (IllegalArgumentException exception) {
                throw new IOException("Invalid saved account at index " + index, exception);
            }
        }
        return accounts;
    }

    public void save(List<AccountIdentity> accounts) throws IOException {
        if (accounts == null) throw new IllegalArgumentException("Account list is required");
        Properties properties = new Properties();
        properties.setProperty("account.count", Integer.toString(accounts.size()));
        for (int index = 0; index < accounts.size(); index++) {
            AccountIdentity account = accounts.get(index);
            if (account == null) throw new IllegalArgumentException("Account cannot be null");
            String prefix = "account." + index + ".";
            properties.setProperty(prefix + "username", account.getUsername());
            properties.setProperty(prefix + "authMode", account.getAuthMode().name());
        }

        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, "msm-accounts-", ".tmp");
        try {
            try (OutputStream output = Files.newOutputStream(temporary)) {
                properties.store(output, "MSM account identities; credentials are not stored");
            }
            try {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
