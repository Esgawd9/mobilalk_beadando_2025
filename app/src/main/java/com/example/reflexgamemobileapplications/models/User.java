package com.example.reflexgamemobileapplications.models;

import java.util.ArrayList;
import java.util.List;

public class User {
    public String username;
    public String email;
    public List<Long> historicalAverages;

    public User() {}

    public User(String username, String email) {
        this.username = username;
        this.email = email;
        this.historicalAverages = new ArrayList<>();
    }
}