package dev.cylox.msm.session;

import dev.cylox.msm.profile.SessionProfile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class SessionManager {
    /** Retained for source compatibility; no configured session count is imposed. */
    @Deprecated public static final int MAX_SESSIONS = Integer.MAX_VALUE;
    private final Map<Long, Session> sessions = new LinkedHashMap<Long, Session>();
    private long nextId = 1L;

    public synchronized Session create(SessionProfile profile) {
        if (profile == null) throw new IllegalArgumentException("Session profile is required");
        if (sessions.size() == Integer.MAX_VALUE) throw new IllegalStateException("The Java collection has reached its maximum capacity");
        for (Session session : sessions.values()) if (session.getProfile().equals(profile)) throw new IllegalArgumentException("That profile is already managed");
        Session session = new Session(nextId++, profile, SessionState.OFFLINE);
        sessions.put(session.getId(), session);
        return session;
    }

    public synchronized Optional<Session> find(long sessionId) { return Optional.ofNullable(sessions.get(sessionId)); }
    public synchronized Optional<Session> findByProfile(SessionProfile profile) {
        if (profile == null) return Optional.empty();
        for (Session session : sessions.values()) if (session.getProfile().equals(profile)) return Optional.of(session);
        return Optional.empty();
    }
    public synchronized List<Session> getSessions() { return Collections.unmodifiableList(new ArrayList<Session>(sessions.values())); }

    public synchronized Session updateState(long sessionId, SessionState nextState) {
        if (nextState == null) throw new IllegalArgumentException("Session state is required");
        Session current = sessions.get(sessionId);
        if (current == null) throw new IllegalArgumentException("Unknown session: " + sessionId);
        if (current.getState() == nextState) return current;
        if (!current.getState().canTransitionTo(nextState)) throw new IllegalStateException("Invalid session state transition: " + current.getState() + " -> " + nextState);
        Session updated = new Session(sessionId, current.getProfile(), nextState);
        sessions.put(sessionId, updated);
        return updated;
    }

    public synchronized boolean remove(long sessionId) {
        Session session = sessions.get(sessionId);
        if (session == null) return false;
        if (session.getState() == SessionState.CONNECTING || session.getState() == SessionState.ONLINE) throw new IllegalStateException("Disconnect the session before removing it");
        sessions.remove(sessionId);
        return true;
    }
    public synchronized int size() { return sessions.size(); }
    public synchronized int remainingCapacity() { return Integer.MAX_VALUE - sessions.size(); }

    public static final class Session {
        private final long id;
        private final SessionProfile profile;
        private final SessionState state;
        private Session(long id, SessionProfile profile, SessionState state) { this.id = id; this.profile = profile; this.state = state; }
        public long getId() { return id; }
        public SessionProfile getProfile() { return profile; }
        public SessionState getState() { return state; }
    }
}
