package com.example.reflexgamemobileapplications.activities;

import android.graphics.Color;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.reflexgamemobileapplications.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameActivity extends AppCompatActivity {
    private final int TARGET_SIZE = 250; // Size of the target
    private final int TOTAL_TARGETS = 30;
    private final int MAX_FALSE_STARTS = 3;

    private static final String TAG = "GameActivity";
    private Button retryButton, mainMenuButton;
    private TextView averageText, roundCounterText, finalAverageText;
    private long startTime;
    private List<Long> reactionTimes = new ArrayList<>();
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private int currentRound = 0;
    private int falseStarts = 0;
    private View targetView;
    private FrameLayout gameLayout;
    private boolean firstTargetClicked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.game_activity);
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance("https://reflexgamemobileapplications-default-rtdb.europe-west1.firebasedatabase.app").getReference();
        gameLayout = findViewById(R.id.gameLayout);
        averageText = findViewById(R.id.averageText);
        roundCounterText = findViewById(R.id.roundCounterText);
        finalAverageText = findViewById(R.id.finalAverageText);

        retryButton = findViewById(R.id.retryButton);
        mainMenuButton = findViewById(R.id.mainMenuButton);

        retryButton.setOnClickListener(v -> retryGame());
        mainMenuButton.setOnClickListener(v -> returnToMainMenu());

        // Initially hidden
        retryButton.setVisibility(View.GONE);
        mainMenuButton.setVisibility(View.GONE);
        finalAverageText.setVisibility(View.GONE);

        updateTargetCounter();

        // load layout
        gameLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                gameLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                setupGame();
            }
        });
    }

    private void updateTargetCounter() {
        roundCounterText.setText("Target " + (currentRound + 1) + "/" + TOTAL_TARGETS);
    }

//    private void onFalseStart() {
//        falseStarts++;
//
//        vibrate();
//
//        if (falseStarts >= MAX_FALSE_STARTS) {
//            reactionTimes.clear();
//            averageText.setText("Too many false starts! Restarting...");
//            falseStarts = 0;
//            currentRound = 0;
//            firstTargetClicked = false; // Reset the flag
//        }
//
//        new Handler().postDelayed(this::setupGame, 2000);
//    }

    private void onTargetClick() {
        vibrate();

        if (!firstTargetClicked) {
            firstTargetClicked = true;
            startTime = System.currentTimeMillis();
        } else {
            long reactionTime = System.currentTimeMillis() - startTime;
            reactionTimes.add(reactionTime);
            startTime = System.currentTimeMillis();
            updateAverage();
        }

        falseStarts = 0;

        if (targetView != null) {
            gameLayout.removeView(targetView);
        }

        currentRound++;

        if (currentRound < TOTAL_TARGETS) {
            setupGame();
        } else {
            endGame();
        }
        updateTargetCounter();
    }

    private void endGame() {
        if (targetView != null) {
            gameLayout.removeView(targetView);
        }
        retryButton.setVisibility(View.VISIBLE);
        mainMenuButton.setVisibility(View.VISIBLE);
        finalAverageText.setVisibility(View.VISIBLE);
        averageText.setVisibility(View.GONE);

        long finalAverage = calculateAverage();
        finalAverageText.setText("Final Average: " + finalAverage + " ms");

        if (mAuth.getCurrentUser() != null) {
            String uid = mAuth.getCurrentUser().getUid();

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
        firstTargetClicked = false;

        retryButton.setVisibility(View.GONE);
        mainMenuButton.setVisibility(View.GONE);
        finalAverageText.setVisibility(View.GONE);
        averageText.setVisibility(View.VISIBLE);

        averageText.setText("");
        updateTargetCounter();

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
        int width = gameLayout.getWidth();
        int height = gameLayout.getHeight();

        Random random = new Random();
        int x = random.nextInt(width - TARGET_SIZE);
        int y = random.nextInt(height - TARGET_SIZE);

        targetView = new View(this);
        ShapeDrawable circle = new ShapeDrawable(new OvalShape());
        circle.getPaint().setColor(Color.GREEN);
        targetView.setBackground(circle);
        targetView.setLayoutParams(new FrameLayout.LayoutParams(TARGET_SIZE, TARGET_SIZE));
        targetView.setX(x);
        targetView.setY(y);
        targetView.setOnClickListener(v -> onTargetClick());

        gameLayout.addView(targetView);
    }

    private void vibrate() {
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(60);
        }
    }
}
