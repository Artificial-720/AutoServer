package me.artificial.autoserver.velocity;

class ServerStatusCache {
    boolean isOnline;
    int playerCount;

    ServerStatusCache(boolean isOnline) {
        this.isOnline = isOnline;
        this.playerCount = 0;
    }
}
