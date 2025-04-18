package com.example.reflexgamemobileapplications.models;

public class LeaderboardEntry {
    private String username;
    private long bestTime;

    public LeaderboardEntry() {}

    public LeaderboardEntry(String username, long bestTime) {
        this.username = username;
        this.bestTime = bestTime;
    }

    public String getUsername() {
        return username;
    }

    public long getBestTime() {
        return bestTime;
    }
}