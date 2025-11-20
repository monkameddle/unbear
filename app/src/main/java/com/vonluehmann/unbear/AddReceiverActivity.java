package com.vonluehmann.unbear;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class AddReceiverActivity extends AppCompatActivity {

    private EditText editLabel;
    private EditText editHost;
    private EditText editPort;
    private EditText editUser;
    private RadioGroup radioAuthMethod;
    private RadioButton radioPassword;
    private RadioButton radioKey;
    private EditText editSshPassword;
    private EditText editPrivateKey;
    private EditText editPrivateKeyPassword;
    private Button buttonImportKey;
    private Button buttonPasteKey;
    private Button buttonDeleteKey;
    private CheckBox checkSendUnlock;
    private EditText editCryptCommand;
    private EditText editLuksPassword;
    private ActivityResultLauncher<String[]> filePickerLauncher;
    private long editId = -1;
    private boolean isEditMode = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_receiver);

        editId = getIntent().getLongExtra("editId", -1);
        isEditMode = (editId != -1);

        editLabel = findViewById(R.id.editLabel);
        editHost = findViewById(R.id.editHost);
        editPort = findViewById(R.id.editPort);
        editUser = findViewById(R.id.editUser);

        radioAuthMethod = findViewById(R.id.radioAuthMethod);
        radioPassword = findViewById(R.id.radioPassword);
        radioKey = findViewById(R.id.radioKey);

        editSshPassword = findViewById(R.id.editSshPassword);
        editPrivateKey = findViewById(R.id.editPrivateKey);
        editPrivateKeyPassword = findViewById(R.id.editPrivateKeyPassword);

        buttonImportKey = findViewById(R.id.buttonImportKey);
        buttonPasteKey = findViewById(R.id.buttonPasteKey);
        buttonDeleteKey = findViewById(R.id.buttonDeleteKey);

        checkSendUnlock = findViewById(R.id.checkSendUnlock);
        editCryptCommand = findViewById(R.id.editCryptCommand);

        editLuksPassword = findViewById(R.id.editLuksPassword);
        Button buttonSave = findViewById(R.id.buttonSave);
        Button buttonCancel = findViewById(R.id.buttonCancel);


        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri != null) {
                        importKeyFromUri(uri);
                    }
                }
        );

        setupAuthSwitching();
        setupCryptUnlockLogic();

        if (!isEditMode) {
            radioPassword.setChecked(true);
        }

        if (isEditMode) {
            loadExistingTarget(editId);
        }


        buttonImportKey.setOnClickListener(v -> filePickerLauncher.launch(new String[]{"*/*"}));

        buttonPasteKey.setOnClickListener(v -> pasteKeyFromClipboard());

        buttonDeleteKey.setOnClickListener(v -> {
            editPrivateKey.setText("");
            Toast.makeText(this, "SSH Key gelöscht", Toast.LENGTH_SHORT).show();
        });

        buttonCancel.setOnClickListener(v -> finish());

        buttonSave.setOnClickListener(v -> saveAndFinish());
    }


    private void setupAuthSwitching() {

        radioAuthMethod.setOnCheckedChangeListener((group, checkedId) -> {

            boolean usingPassword = checkedId == R.id.radioPassword;
            boolean usingKey = checkedId == R.id.radioKey;

            if (usingPassword) {
                editSshPassword.setVisibility(View.VISIBLE);

                editPrivateKey.setVisibility(View.GONE);
                editPrivateKeyPassword.setVisibility(View.GONE);
                buttonImportKey.setVisibility(View.GONE);
                buttonPasteKey.setVisibility(View.GONE);
                buttonDeleteKey.setVisibility(View.GONE);
            }

            if (usingKey) {
                editSshPassword.setVisibility(View.GONE);

                editPrivateKey.setVisibility(View.VISIBLE);
                editPrivateKeyPassword.setVisibility(View.VISIBLE);
                buttonImportKey.setVisibility(View.VISIBLE);
                buttonPasteKey.setVisibility(View.VISIBLE);
                buttonDeleteKey.setVisibility(View.VISIBLE);
            }
        });
    }


    private void setupCryptUnlockLogic() {

        editCryptCommand.setEnabled(false);

        checkSendUnlock.setOnCheckedChangeListener((v, checked) -> {
            editCryptCommand.setEnabled(checked);

            if (checked && editCryptCommand.getText().toString().trim().isEmpty()) {
                editCryptCommand.setText("cryptroot-unlock");
            }
        });
    }


    private void loadExistingTarget(long id) {

        CredentialStorage storage = new CredentialStorage(this);
        TargetHost t = storage.getTargetById(id);

        if (t == null) return;

        editLabel.setText(t.getLabel());
        editHost.setText(t.getHost());
        editPort.setText(String.valueOf(t.getPort()));
        editUser.setText(t.getUser());

        if (t.isUseSshPassword()) {
            radioPassword.setChecked(true);
            editSshPassword.setText(t.getSshPassword());
        }

        if (t.isUseSshKey()) {
            radioKey.setChecked(true);
            editPrivateKey.setText(t.getSshPrivateKey());
            editPrivateKeyPassword.setText(t.getSshPrivateKeyPassword());
        }

        checkSendUnlock.setChecked(t.isSendCryptrootUnlock());
        editCryptCommand.setText(t.getCryptrootCommand());
        editCryptCommand.setEnabled(t.isSendCryptrootUnlock());

        editLuksPassword.setText(t.getLuksPassword());
    }


    private void saveAndFinish() {

        CredentialStorage storage = new CredentialStorage(this);
        List<TargetHost> list = storage.loadTargets();
        if (list == null) list = new ArrayList<>();

        long id = isEditMode ? editId : storage.generateNewId(list);

        boolean sshPasswordMode = radioPassword.isChecked();
        boolean sshKeyMode = radioKey.isChecked();

        TargetHost.Builder builder = new TargetHost.Builder()
                .setId(id)
                .setLabel(editLabel.getText().toString().trim())
                .setHost(editHost.getText().toString().trim())
                .setPort(Integer.parseInt(editPort.getText().toString().trim()))
                .setUser(editUser.getText().toString().trim().isEmpty() ? "root" : editUser.getText().toString().trim())
                .setSendCryptrootUnlock(checkSendUnlock.isChecked())
                .setCryptrootCommand(editCryptCommand.getText().toString().trim())
                .setLuksPassword(editLuksPassword.getText().toString().trim());

        if (sshPasswordMode) {
            builder.useSshPassword(editSshPassword.getText().toString());
        }

        if (sshKeyMode) {
            builder.useSshKey(
                    editPrivateKey.getText().toString(),
                    editPrivateKeyPassword.getText().toString()
            );
        }

        TargetHost created = builder.build();

        if (isEditMode) {
            storage.replaceTarget(created);
        } else {
            list.add(created);
            storage.saveTargets(list);
        }

        setResult(RESULT_OK);
        finish();
    }


    private void importKeyFromUri(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            if (is == null) return;

            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();

            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }

            reader.close();
            is.close();

            String key = sb.toString().trim();

            if (key.contains("PRIVATE KEY")) {
                editPrivateKey.setText(key);
                Toast.makeText(this, "Key importiert", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Ungültiges Key Format", Toast.LENGTH_LONG).show();
            }

        } catch (Exception e) {
            Toast.makeText(this, "Fehler: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }


    private void pasteKeyFromClipboard() {

        ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (cm == null || !cm.hasPrimaryClip()) {
            Toast.makeText(this, "Zwischenablage leer", Toast.LENGTH_SHORT).show();
            return;
        }

        ClipData clip = cm.getPrimaryClip();
        if (clip == null || clip.getItemCount() == 0) {
            Toast.makeText(this, "Zwischenablage leer", Toast.LENGTH_SHORT).show();
            return;
        }

        CharSequence text = clip.getItemAt(0).getText();
        if (text == null) {
            Toast.makeText(this, "Kein Text in Zwischenablage", Toast.LENGTH_SHORT).show();
            return;
        }

        String key = text.toString().trim();

        if (!key.contains("PRIVATE KEY")) {
            Toast.makeText(this, "Ungültiger SSH Key", Toast.LENGTH_LONG).show();
            return;
        }

        editPrivateKey.setText(key);
        Toast.makeText(this, "SSH Key eingefügt", Toast.LENGTH_SHORT).show();
    }
}
