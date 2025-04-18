package com.example.reflexgamemobileapplications.activities;

import android.graphics.Color;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.reflexgamemobileapplications.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.ArrayList;
import java.util.List;

public class GameActivity extends AppCompatActivity {

    private static final String TAG = "GameActivity";
    private Button targetButton, retryButton, mainMenuButton;
    private TextView averageText, roundCounterText;
    private long startTime;
    private List<Long> reactionTimes = new ArrayList<>();
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private int currentRound = 0;
    private final int TOTAL_ROUNDS = 5;
    private int falseStarts = 0;
    private final int MAX_FALSE_STARTS = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.game_activity);
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance("https://reflexgamemobileapplications-default-rtdb.europe-west1.firebasedatabase.app").getReference();
        targetButton = findViewById(R.id.targetButton);
        averageText = findViewById(R.id.averageText);
        roundCounterText = findViewById(R.id.roundCounterText);

        retryButton = findViewById(R.id.retryButton);
        mainMenuButton = findViewById(R.id.mainMenuButton);

        retryButton.setOnClickListener(v -> retryGame());
        mainMenuButton.setOnClickListener(v -> returnToMainMenu());

        // Initially hide these buttons
        retryButton.setVisibility(View.GONE);
        mainMenuButton.setVisibility(View.GONE);

        targetButton.setOnClickListener(v -> {
            if (targetButton.isEnabled()) {
                if (targetButton.getText().toString().equals("Wait for green...")) {
                    onFalseStart();
                } else {
                    onTargetClick();
                }
            }
        });

        updateRoundCounter();
        setupGame();
    }

    private void updateRoundCounter() {
        roundCounterText.setText("Round " + (currentRound + 1) + "/" + TOTAL_ROUNDS);
    }

    private void onFalseStart() {
        falseStarts++;

        playGeneratedBeep(100);
        vibrate(100);
        targetButton.setText("TOO SOON! (" + falseStarts + "/" + MAX_FALSE_STARTS + ")");
        targetButton.setBackgroundColor(Color.YELLOW);
        targetButton.setEnabled(false);

        if (falseStarts >= MAX_FALSE_STARTS) {
            reactionTimes.clear();
            averageText.setText("Too many false starts! Restarting...");
            falseStarts = 0;
            currentRound = 0;
        }

        new Handler().postDelayed(this::setupGame, 2000);
    }

    private void onTargetClick() {
        vibrate(60);
        playGeneratedBeep(60);
        long reactionTime = System.currentTimeMillis() - startTime;
        reactionTimes.add(reactionTime);
        falseStarts = 0;

        targetButton.setBackgroundColor(Color.BLUE);
        targetButton.setText(reactionTime + " ms");
        updateAverage();

        saveReactionTime(reactionTime);

        targetButton.setEnabled(false);
        currentRound++;

        if (currentRound < TOTAL_ROUNDS) {
            new Handler().postDelayed(this::setupGame, 1500);
        } else {
            endGame();
        }
        updateRoundCounter();
    }

    private void endGame() {
        targetButton.setEnabled(false);
        targetButton.setText("Game Over!");
        targetButton.setBackgroundColor(Color.LTGRAY);
        retryButton.setVisibility(View.VISIBLE);
        mainMenuButton.setVisibility(View.VISIBLE);

        long finalAverage = calculateAverage();
        averageText.setText("Final Average: " + finalAverage + " ms");

        if (mAuth.getCurrentUser() != null) {
            String uid = mAuth.getCurrentUser().getUid();

            mDatabase.child("users").child(uid).child("allReactionTimes")
                    .setValue(reactionTimes);

            mDatabase.child("users").child(uid).child("historicalAverages")
                    .push().setValue(finalAverage)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Saved final average to historical list");
                        }
                    });
        }
    }

    private void retryGame() {
        reactionTimes.clear();
        currentRound = 0;
        falseStarts = 0;

        retryButton.setVisibility(View.GONE);
        mainMenuButton.setVisibility(View.GONE);

        averageText.setText("");
        updateRoundCounter();

        setupGame();
    }

    private void returnToMainMenu() {
        finish();
    }

    private long calculateAverage() {
        if (reactionTimes.isEmpty()) return 0;
        long sum = 0;
        for (long time : reactionTimes) {
            sum += time;
        }
        return sum / reactionTimes.size();
    }

    private void updateAverage() {
        if (!reactionTimes.isEmpty()) {
            long average = calculateAverage();
            averageText.setText("Average: " + average + " ms");
        }
    }

    private void setupGame() {
        targetButton.setBackgroundColor(Color.RED);
        targetButton.setText("Wait for green...");
        targetButton.setEnabled(true);

        new Handler().postDelayed(() -> {
            if (currentRound < TOTAL_ROUNDS && targetButton.getText().toString().equals("Wait for green...")) {
                startTime = System.currentTimeMillis();
                targetButton.setBackgroundColor(Color.GREEN);
                vibrate(10);
                targetButton.setText("CLICK NOW!");
            }
        }, 1000 + (long)(Math.random() * 4000));
    }

    private void saveReactionTime(long reactionTime) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            mDatabase.child("users").child(uid).child("reactionTimes")
                    .push().setValue(reactionTime);
        }
    }

    private void vibrate(int time) {
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(time);
        }
    }

    private void playGeneratedBeep(int time) {
        ToneGenerator toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
        toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, time); // 500ms beep
    }
}