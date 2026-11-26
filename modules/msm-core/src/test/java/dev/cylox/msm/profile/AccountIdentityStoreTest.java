package dev.cylox.msm.profile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class AccountIdentityStoreTest {
    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("msm-account-test");
        Path file = directory.resolve("accounts.properties");
        AccountIdentityStore store = new AccountIdentityStore(file);
        try {
            if (!store.load().isEmpty()) throw new AssertionError("Missing account file must load empty");
            List<AccountIdentity> accounts = Arrays.asList(
                    new AccountIdentity("OfflineOne", SessionAuthMode.OFFLINE_LOCAL),
                    new AccountIdentity("PlayerTwo", SessionAuthMode.MICROSOFT));
            store.save(accounts);
            if (!accounts.equals(store.load())) throw new AssertionError("Account identities did not round-trip");
            store.save(Collections.<AccountIdentity>emptyList());
            if (!store.load().isEmpty()) throw new AssertionError("Empty account list was not saved");
            Files.write(file, Arrays.asList("account.count=1",
                    "account.0.username=Legacy", "account.0.authMode=OFFLINE_LOCAL"),
                    StandardCharsets.ISO_8859_1);
            if (!store.load().get(0).getUsername().equals("Legacy")) {
                throw new AssertionError("Stored identity was not loaded");
            }
            expectInvalid(store, file, "account.count=x\n", "Invalid account count");
            expectInvalid(store, file, "account.count=-1\n", "Account count is out of range");
            expectInvalid(store, file, "account.count=1\naccount.0.username=Bad Name\naccount.0.authMode=OFFLINE_LOCAL\n",
                    "Invalid saved account at index 0");
            expectInvalid(store, file, "account.count=1\naccount.0.username=Player\naccount.0.authMode=UNKNOWN\n",
                    "Invalid saved account at index 0");
        } finally {
            Files.deleteIfExists(file);
            Files.deleteIfExists(directory);
        }
        System.out.println("AccountIdentityStoreTest: PASS");
    }

    private static void expectInvalid(AccountIdentityStore store, Path file, String value,
                                      String expectedMessage) throws Exception {
        Files.write(file, value.getBytes(StandardCharsets.ISO_8859_1));
        try {
            store.load();
            throw new AssertionError("Expected IOException: " + expectedMessage);
        } catch (IOException expected) {
            if (!expected.getMessage().contains(expectedMessage)) {
                throw new AssertionError("Unexpected error: " + expected.getMessage(), expected);
            }
        }
    }
}
