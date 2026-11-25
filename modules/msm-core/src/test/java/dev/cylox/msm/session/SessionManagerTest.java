package dev.cylox.msm.session;

import dev.cylox.msm.profile.SessionProfile;

public final class SessionManagerTest {
    public static void main(String[] args) {
        SessionManager manager = new SessionManager();
        SessionProfile firstProfile = new SessionProfile("PlayerOne", "play.example.net");
        SessionManager.Session first = manager.create(firstProfile);
        check(first.getState() == SessionState.OFFLINE, "New session must be offline");
        check(manager.find(first.getId()).isPresent(), "Created session must be findable");
        check(manager.findByProfile(firstProfile).isPresent(), "Session must be findable by profile");
        check(!manager.findByProfile(new SessionProfile("OtherPlayer", "play.example.net")).isPresent(), "Unknown profile must not be found");
        check(!manager.findByProfile(null).isPresent(), "Null profile lookup must be empty");
        expectIllegalArgument(new Action() { public void run() { manager.create(firstProfile); } }, "Duplicate profile must be rejected");
        manager.updateState(first.getId(), SessionState.CONNECTING);
        manager.updateState(first.getId(), SessionState.ONLINE);
        expectIllegalState(new Action() { public void run() { manager.remove(first.getId()); } }, "Online session must not be removed");
        expectIllegalState(new Action() { public void run() { manager.updateState(first.getId(), SessionState.CONNECTING); } }, "Invalid state transition must be rejected");
        manager.updateState(first.getId(), SessionState.DISCONNECTED);
        check(manager.remove(first.getId()), "Disconnected session must be removable");
        check(!manager.find(first.getId()).isPresent(), "Removed session must not be findable");
        for (int index = 0; index < 128; index++) manager.create(new SessionProfile("Player" + index, "server" + index + ".example.net"));
        check(manager.size() == 128, "Manager must accept more than the previous 20-session limit");
        check(manager.remainingCapacity() > 0, "Manager must not impose a fixed session limit");
        System.out.println("SessionManagerTest: PASS");
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private static void expectIllegalArgument(Action action, String message) { try { action.run(); } catch (IllegalArgumentException expected) { return; } throw new AssertionError(message); }
    private static void expectIllegalState(Action action, String message) { try { action.run(); } catch (IllegalStateException expected) { return; } throw new AssertionError(message); }
    private interface Action { void run(); }
}
