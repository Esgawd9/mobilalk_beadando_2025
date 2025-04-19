package com.example.reflexgamemobileapplications.activities;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.reflexgamemobileapplications.R;
import com.example.reflexgamemobileapplications.adapters.LeaderboardAdapter;
import com.example.reflexgamemobileapplications.models.LeaderboardEntry;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class LeaderboardActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private DatabaseReference mDatabase;
    private List<LeaderboardEntry> leaderboardEntries = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        recyclerView = findViewById(R.id.leaderboardRecyclerView);
        progressBar = findViewById(R.id.progressBar);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new LeaderboardAdapter(leaderboardEntries));

        mDatabase = FirebaseDatabase.getInstance("https://reflexgamemobileapplications-default-rtdb.europe-west1.firebasedatabase.app").getReference("users");
        loadLeaderboardData();
    }

    private void loadLeaderboardData() {
        mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                leaderboardEntries.clear();

                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    String username = userSnapshot.child("username").getValue(String.class);
                    List<Long> reactionTimes = new ArrayList<>();

                    // Get all reaction times
                    for (DataSnapshot timeSnapshot : userSnapshot.child("historicalAverages").getChildren()) {
                        Long time = timeSnapshot.getValue(Long.class);
                        if (time != null) {
                            reactionTimes.add(time);
                        }
                    }

                    // Calculate best time
                    if (!reactionTimes.isEmpty()) {
                        long bestTime = Collections.min(reactionTimes);
                        leaderboardEntries.add(new LeaderboardEntry(username, bestTime));
                    }
                }

                // Sort by best time (ascending)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Collections.sort(leaderboardEntries, Comparator.comparingLong(LeaderboardEntry::getBestTime));
                }

                updateUI();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(LeaderboardActivity.this,
                        "Failed to load leaderboard", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI() {
        progressBar.setVisibility(View.GONE);
        recyclerView.setAdapter(new LeaderboardAdapter(leaderboardEntries));
    }
}
