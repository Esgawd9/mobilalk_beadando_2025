package com.example.reflexgamemobileapplications.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.reflexgamemobileapplications.R;
import com.example.reflexgamemobileapplications.models.LeaderboardEntry;

import java.util.List;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder> {
    private List<LeaderboardEntry> entries;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView positionText, usernameText, scoreText;

        public ViewHolder(View itemView) {
            super(itemView);
            positionText = itemView.findViewById(R.id.positionText);
            usernameText = itemView.findViewById(R.id.usernameText);
            scoreText = itemView.findViewById(R.id.scoreText);
        }
    }

    public LeaderboardAdapter(List<LeaderboardEntry> entries) {
        this.entries = entries;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.leaderboard_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        LeaderboardEntry entry = entries.get(position);
        holder.positionText.setText(String.valueOf(position + 1));
        holder.usernameText.setText(entry.getUsername());
        holder.scoreText.setText(entry.getBestTime() + " ms");
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }
}