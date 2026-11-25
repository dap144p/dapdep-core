package dev.cylox.msm.profile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class SessionProfileStoreTest {
    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("msm-profile-test");
        Path file = directory.resolve("profiles.properties");
        SessionProfileStore store = new SessionProfileStore(file);
        try {
            if (!store.load().isEmpty()) throw new AssertionError("Missing file must load as empty");
            List<SessionProfile> profiles = Arrays.asList(new SessionProfile("Player_B", "play.example.net:25565", SessionAuthMode.MICROSOFT), new SessionProfile("Player_C", "mc.example.org", SessionAuthMode.OFFLINE_LOCAL));
            store.save(profiles);
            if (!profiles.equals(store.load())) throw new AssertionError("Saved profiles did not round-trip");
            store.save(Collections.<SessionProfile>emptyList());
            if (!store.load().isEmpty()) throw new AssertionError("Cleared profile list was not saved");
            List<SessionProfile> manyProfiles = new ArrayList<SessionProfile>();
            for (int index = 0; index < 150; index++) manyProfiles.add(new SessionProfile("Player" + index, "server" + index + ".example.net", SessionAuthMode.OFFLINE_LOCAL));
            store.save(manyProfiles);
            if (!manyProfiles.equals(store.load())) throw new AssertionError("Profile storage must support more than the previous 100-profile limit");
            Files.write(file, Arrays.asList("profile.count=1", "profile.0.username=LegacyPlayer", "profile.0.server=legacy.example.net"), StandardCharsets.ISO_8859_1);
            List<SessionProfile> legacyProfiles = store.load();
            if (legacyProfiles.size() != 1 || legacyProfiles.get(0).getAuthMode() != SessionAuthMode.MICROSOFT) throw new AssertionError("Legacy profiles must default to Microsoft authentication");
            expectInvalidFile(store, file, "", "Missing profile count");
            expectInvalidFile(store, file, "profile.0.username=Player\n", "Missing profile count");
            expectInvalidFile(store, file, "profile.count=not-a-number\n", "Invalid profile count");
            expectInvalidFile(store, file, "profile.count=-1\n", "Profile count is out of range");
            expectInvalidFile(store, file, "profile.count=101\n", "Profile count does not match");
            expectInvalidFile(store, file, "profile.count=1\nprofile.0.username=Player\n", "Invalid saved profile at index 0");
            String profile = "profile.count=1\nprofile.0.username=Player\nprofile.0.server=example.net\n";
            expectInvalidFile(store, file, profile + "profile.0.authMode=FUTURE_MODE\n", "Invalid saved profile at index 0");
            expectInvalidFile(store, file, profile + "profile.0.authMode=\n", "Invalid saved profile at index 0");
            Files.write(file, (profile + "profile.0.authMode= offline_local  \n").getBytes(StandardCharsets.ISO_8859_1));
            if (store.load().get(0).getAuthMode() != SessionAuthMode.OFFLINE_LOCAL) throw new AssertionError("Known auth modes should accept whitespace and case differences");
            String malformedUnicode = "profile.count=" + "\\" + "u12G4\n";
            expectInvalidFile(store, file, malformedUnicode, "Invalid profile properties");
            try { new SessionProfileStore(directory).load(); throw new AssertionError("A directory cannot be loaded as a profile store"); }
            catch (IOException expected) { }
            testDanglingSymbolicLink(directory);
        } finally { Files.deleteIfExists(file); Files.deleteIfExists(directory); }
        assertRejected(new Runnable() { public void run() { new SessionProfile(" ", "play.example.net"); } }, "Blank username must be rejected");
        assertRejected(new Runnable() { public void run() { new SessionProfile("Player Name", "play.example.net"); } }, "Invalid username must be rejected");
        assertRejected(new Runnable() { public void run() { new SessionProfile("Player", "play.example.net", null); } }, "Missing auth mode must be rejected");
        System.out.println("SessionProfileStoreTest: PASS");
    }
    private static void assertRejected(Runnable action, String message) { try { action.run(); } catch (IllegalArgumentException expected) { return; } throw new AssertionError(message); }
    private static void testDanglingSymbolicLink(Path directory) throws Exception {
        Path link = directory.resolve("broken-link.properties");
        try { Files.createSymbolicLink(link, directory.resolve("missing-target.properties")); }
        catch (UnsupportedOperationException | IOException | SecurityException unsupported) { return; }
        try { try { new SessionProfileStore(link).load(); throw new AssertionError("A dangling symbolic link must not load as empty"); } catch (IOException expected) { } }
        finally { Files.deleteIfExists(link); }
    }
    private static void expectInvalidFile(SessionProfileStore store, Path file, String contents, String expectedMessage) throws Exception {
        byte[] original = contents.getBytes(StandardCharsets.ISO_8859_1);
        Files.write(file, original);
        try { store.load(); throw new AssertionError("Expected IOException: " + expectedMessage); }
        catch (IOException expected) { if (!expected.getMessage().contains(expectedMessage)) throw new AssertionError("Unexpected error: " + expected.getMessage(), expected); }
        if (!Arrays.equals(original, Files.readAllBytes(file))) throw new AssertionError("Loading an invalid file must not change it");
    }
}
