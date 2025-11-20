package com.vonluehmann.unbear;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TargetAdapter extends RecyclerView.Adapter<TargetAdapter.ViewHolder> {

    private List<TargetHost> targets;
    private final Listener listener;

    public interface Listener {
        void onEdit(TargetHost target);
        void onUnlock(TargetHost target);
        void onDelete(TargetHost target);
    }

    public TargetAdapter(List<TargetHost> targets, Listener listener) {
        this.targets = targets;
        this.listener = listener;
    }

    public void updateList(List<TargetHost> newTargets) {
        this.targets = newTargets;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_target, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TargetHost t = targets.get(position);

        holder.textLabel.setText(t.getLabel());
        holder.textHost.setText(t.getHost());

        holder.buttonUnlock.setOnClickListener(v -> {
            if (listener != null) listener.onUnlock(t);
        });

        holder.buttonEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(t);
        });

        holder.buttonDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(t);
        });
    }

    @Override
    public int getItemCount() {
        return targets != null ? targets.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView textLabel;
        TextView textHost;

        ImageButton buttonUnlock;
        ImageButton buttonEdit;
        ImageButton buttonDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            textLabel = itemView.findViewById(R.id.textLabel);
            textHost = itemView.findViewById(R.id.textHost);

            buttonUnlock = itemView.findViewById(R.id.buttonUnlock);
            buttonEdit = itemView.findViewById(R.id.buttonEdit);
            buttonDelete = itemView.findViewById(R.id.buttonDelete);
        }
    }
}
