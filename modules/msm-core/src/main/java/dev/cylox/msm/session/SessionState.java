package dev.cylox.msm.session;

public enum SessionState {
    OFFLINE, CONNECTING, ONLINE, DISCONNECTED, FAILED;
    public boolean canTransitionTo(SessionState next) {
        if (next == null || next == this) return false;
        switch (this) {
            case OFFLINE: return next == CONNECTING;
            case CONNECTING: return next == ONLINE || next == DISCONNECTED || next == FAILED;
            case ONLINE: return next == DISCONNECTED || next == FAILED;
            case DISCONNECTED:
            case FAILED: return next == CONNECTING;
            default: return false;
        }
    }
}
