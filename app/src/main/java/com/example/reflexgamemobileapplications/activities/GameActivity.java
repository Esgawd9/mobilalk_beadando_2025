package com.example.reflexgamemobileapplications.activities;

import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
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
    // Configurable
    private int validTargetColor = 0xff007BFF; // Valid target color
    private int invalidTargetColor = 0xffFF5733; // Invalid target color
    private final int TARGET_SIZE = 250; // Size of the target
    private final int TOTAL_TARGETS = 60; // Total target count
    private final int MAX_FAILS = 3; // Maximum failed clicks

    private static final String TAG = "GameActivity";
    private Button retryButton, mainMenuButton;
    private TextView averageText, roundCounterText, finalAverageText;
    private long startTime;
    private List<Long> reactionTimes = new ArrayList<>();
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private int currentRound = 0;
    private int fails = 0;
    private View greenTargetView, yellowTargetView;
    private FrameLayout gameLayout;
    private boolean firstTargetClicked = false;
    private boolean failed = false;
    private MediaPlayer clickSound;

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

        // Initially hide
        retryButton.setVisibility(View.GONE);
        mainMenuButton.setVisibility(View.GONE);
        finalAverageText.setVisibility(View.GONE);

//        clickSound = MediaPlayer.create(this, R.raw.click_sound_2);

        updateTargetCounter();

        // Load layout
        gameLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                gameLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                setupGame();
                enableImmersiveMode();
            }
        });
    }

    private void updateTargetCounter() {
        roundCounterText.setText("Target " + (currentRound + 1) + "/" + TOTAL_TARGETS);
    }

    private void onFalseClick() {
        fails++;

        vibrate(100);

        if (fails >= MAX_FAILS) {
            reactionTimes.clear();
            averageText.setText("");
            fails = 0;
            currentRound = 0;
            firstTargetClicked = false; // Reset
            failed = true;
            endGame();
        }
    }

    private void onTargetClick(View targetView) {
        vibrate(60);

        if (clickSound != null) {
            if (clickSound.isPlaying()) {
                clickSound.seekTo(0);
            }
            clickSound.start();
        }

        if (targetView == yellowTargetView) {
            onFalseClick();
            return;
        }

        if (!firstTargetClicked) {
            firstTargetClicked = true;
            startTime = System.currentTimeMillis(); // Start counting
        } else {
            long reactionTime = System.currentTimeMillis() - startTime;
            reactionTimes.add(reactionTime);
            startTime = System.currentTimeMillis(); // Reset for next target
            updateAverage();
        }

        gameLayout.removeView(greenTargetView);
        greenTargetView = yellowTargetView;
        ShapeDrawable greenCircle = new ShapeDrawable(new OvalShape());
        greenCircle.getPaint().setColor(validTargetColor);
        greenTargetView.setBackground(greenCircle);
        greenTargetView.setOnClickListener(v -> onTargetClick(greenTargetView));

        currentRound++;

        if (currentRound < TOTAL_TARGETS) {
            spawnYellowTarget();
        } else {
            endGame();
        }
        updateTargetCounter();
    }

    private void endGame() {
        gameLayout.removeView(greenTargetView);
        gameLayout.removeView(yellowTargetView);
        retryButton.setVisibility(View.VISIBLE);
        mainMenuButton.setVisibility(View.VISIBLE);
        finalAverageText.setVisibility(View.VISIBLE);
        roundCounterText.setVisibility(View.GONE);
        averageText.setText("");

        long finalAverage = calculateAverage();
        if (!failed) {
            finalAverageText.setText("Final Average: " + finalAverage + " ms");
        } else {
            finalAverageText.setText("Too many failed clicks!");
        }

        if (mAuth.getCurrentUser() != null && finalAverage != 0) {
            String uid = mAuth.getCurrentUser().getUid();

            mDatabase.child("users").child(uid).child("historicalAverages")
                    .push().setValue(finalAverage)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Saved final average to historical list");
                        }
                    });
        }
        disableImmersiveMode();
    }

    private void retryGame() {
        reactionTimes.clear();
        currentRound = 0;
        fails = 0;
        firstTargetClicked = false;
        failed = false;
        updateTargetCounter();

        retryButton.setVisibility(View.GONE);
        mainMenuButton.setVisibility(View.GONE);
        finalAverageText.setVisibility(View.GONE);
        roundCounterText.setVisibility(View.VISIBLE);

        setupGame();
        enableImmersiveMode();
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
        failed = false;
        spawnGreenTarget();
        spawnYellowTarget();
    }

    private void spawnGreenTarget() {
        int width = gameLayout.getWidth();
        int height = gameLayout.getHeight();

        Random random = new Random();
        int x, y;
        do {
            x = random.nextInt(width - TARGET_SIZE);
            y = random.nextInt(height - TARGET_SIZE);
        } while (isOverlapping(x, y, greenTargetView) || isOverlapping(x, y, yellowTargetView));

        greenTargetView = new View(this);
        ShapeDrawable greenCircle = new ShapeDrawable(new OvalShape());
        greenCircle.getPaint().setColor(validTargetColor);
        greenTargetView.setBackground(greenCircle);
        greenTargetView.setLayoutParams(new FrameLayout.LayoutParams(TARGET_SIZE, TARGET_SIZE));
        greenTargetView.setX(x);
        greenTargetView.setY(y);
        greenTargetView.setOnClickListener(v -> onTargetClick(greenTargetView));

        gameLayout.addView(greenTargetView);
    }

    private void spawnYellowTarget() {
        int width = gameLayout.getWidth();
        int height = gameLayout.getHeight();

        Random random = new Random();
        int x, y;
        do {
            x = random.nextInt(width - TARGET_SIZE);
            y = random.nextInt(height - TARGET_SIZE);
        } while (isOverlapping(x, y, greenTargetView) || isOverlapping(x, y, yellowTargetView));

        yellowTargetView = new View(this);
        ShapeDrawable yellowCircle = new ShapeDrawable(new OvalShape());
        yellowCircle.getPaint().setColor(invalidTargetColor);
        yellowTargetView.setBackground(yellowCircle);
        yellowTargetView.setLayoutParams(new FrameLayout.LayoutParams(TARGET_SIZE, TARGET_SIZE));
        yellowTargetView.setX(x);
        yellowTargetView.setY(y);
        yellowTargetView.setOnClickListener(v -> onTargetClick(yellowTargetView));

        gameLayout.addView(yellowTargetView);
    }

    private boolean isOverlapping(int x, int y, View view) {
        if (view == null) return false;
        int viewX = (int) view.getX();
        int viewY = (int) view.getY();
        return !(x >= viewX + TARGET_SIZE || x + TARGET_SIZE <= viewX || y >= viewY + TARGET_SIZE || y + TARGET_SIZE <= viewY);
    }

    private void vibrate(int time) {
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(time);
        }
    }

    private void enableImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().getInsetsController().hide(WindowInsets.Type.systemBars());
            getWindow().getInsetsController().setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }
    }

    private void disableImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().getInsetsController().show(WindowInsets.Type.systemBars());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Release media player
        if (clickSound != null) {
            clickSound.release();
        }
    }
}
