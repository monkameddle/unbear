package com.vonluehmann.unbear;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerTargets;
    private FloatingActionButton fabAdd;

    private ActivityResultLauncher<Intent> addReceiverLauncher;

    private List<TargetHost> targets = new ArrayList<>();
    private TargetAdapter adapter;

    private CredentialStorage credStorage;
    private boolean unlocked = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerTargets = findViewById(R.id.recyclerTargets);
        fabAdd = findViewById(R.id.fabAdd);

        credStorage = new CredentialStorage(this);
        BiometricHelper biometricHelper = new BiometricHelper(this);

        addReceiverLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        reloadTargets();
                    }
                }
        );

        int can = BiometricManager.from(this).canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG
                        | BiometricManager.Authenticators.DEVICE_CREDENTIAL
        );

        if (can == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) {
            showSetupHint();
            return;
        }

        if (can != BiometricManager.BIOMETRIC_SUCCESS) {
            initializeApp();
            return;
        }

        biometricHelper.authenticate(new BiometricHelper.Callback() {
            @Override
            public void onAuthenticated() {
                unlocked = true;
                initializeApp();
            }

            @Override
            public void onFailed() {
                finish();
            }
        });
    }


    private void initializeApp() {
        recyclerTargets.setLayoutManager(new LinearLayoutManager(this));
        reloadTargets();

        fabAdd.setOnClickListener(v -> {
            Intent i = new Intent(MainActivity.this, AddReceiverActivity.class);
            addReceiverLauncher.launch(i);
        });
    }


    private void reloadTargets() {
        targets = credStorage.loadTargets();
        if (targets == null) {
            targets = new ArrayList<>();
        }

        if (targets.isEmpty()) {
            TargetHost demo = new TargetHost.Builder()
                    .setId(1)
                    .setLabel("Demo Server")
                    .setHost("192.168.0.10")
                    .setPort(22)
                    .setUser("root")
                    .setSendCryptrootUnlock(true)
                    .setCryptrootCommand("cryptroot-unlock")
                    .build();

            targets.add(demo);
            credStorage.saveTargets(targets);
        }

        adapter = new TargetAdapter(targets, new TargetAdapter.Listener() {
            @Override
            public void onEdit(TargetHost target) {
                Intent i = new Intent(MainActivity.this, AddReceiverActivity.class);
                i.putExtra("editId", target.getId());
                addReceiverLauncher.launch(i);
            }

            @Override
            public void onUnlock(TargetHost target) {
                showUnlockDialog(target);
            }

            @Override
            public void onDelete(TargetHost target) {
                confirmDelete(target);
            }
        });

        recyclerTargets.setAdapter(adapter);
    }


    private void showUnlockDialog(TargetHost target) {

        LayoutInflater inflater = LayoutInflater.from(MainActivity.this);
        View dialogView = inflater.inflate(R.layout.dialog_unlock_log, null);

        TextView textProgress = dialogView.findViewById(R.id.textProgress);
        TextView textLog = dialogView.findViewById(R.id.textLog);
        ProgressBar progressBar = dialogView.findViewById(R.id.progressBar);

        AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
                .setTitle("Entsperre " + target.getLabel())
                .setView(dialogView)
                .setCancelable(false)
                .create();

        dialog.show();

        UnlockService unlocker = new UnlockService();

        unlocker.unlock(target, new UnlockService.Callback() {

            @Override
            public void onProgress(String msg) {
                runOnUiThread(() -> textProgress.setText(msg));
            }

            @Override
            public void onLog(String line) {
                runOnUiThread(() -> {
                    String old = textLog.getText().toString();
                    textLog.setText(old + line + "\n");
                });
            }

            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    textProgress.setText("Erfolg");
                    textProgress.postDelayed(() -> dialog.dismiss(), 2000);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    textProgress.setText(error);
                    textProgress.postDelayed(() -> dialog.dismiss(), 2000);
                });
            }
        });
    }


    private void confirmDelete(TargetHost target) {

        new AlertDialog.Builder(this)
                .setTitle("Eintrag löschen")
                .setMessage("Möchtest du '" + target.getLabel() + "' wirklich löschen?")
                .setPositiveButton("Löschen", (d, w) -> deleteTarget(target))
                .setNegativeButton("Abbrechen", null)
                .show();
    }


    private void deleteTarget(TargetHost target) {

        List<TargetHost> list = credStorage.loadTargets();
        if (list == null) return;

        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId() == target.getId()) {
                list.remove(i);
                break;
            }
        }

        credStorage.saveTargets(list);

        targets = list;
        adapter.updateList(list);
        adapter.notifyDataSetChanged();

        Toast.makeText(this, "Gelöscht", Toast.LENGTH_SHORT).show();
    }


    private void showSetupHint() {
        new AlertDialog.Builder(this)
                .setTitle("Sicherheit erforderlich")
                .setMessage(
                        "Bitte richte entweder Biometrie oder einen Sperrbildschirm Code ein, " +
                                "damit deine Entsperrdaten sicher geschützt werden können."
                )
                .setPositiveButton("OK", (d, w) -> finish())
                .setCancelable(false)
                .show();
    }


}