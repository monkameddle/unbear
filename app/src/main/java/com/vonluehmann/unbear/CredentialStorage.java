package com.vonluehmann.unbear;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class CredentialStorage {

    private static final String KEY_ALIAS = "unbear_targets_key";
    private static final String FILE_NAME = "targets.json.enc";

    private final Context context;

    public CredentialStorage(Context context) {
        this.context = context;
        ensureKeyExists();
    }


    private void ensureKeyExists() {
        try {
            KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);

            if (!ks.containsAlias(KEY_ALIAS)) {
                KeyGenParameterSpec spec =
                        new KeyGenParameterSpec.Builder(
                                KEY_ALIAS,
                                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT
                        )
                                .setKeySize(256)
                                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                                .setUserAuthenticationRequired(false)
                                .build();

                KeyGenerator gen = KeyGenerator.getInstance("AES", "AndroidKeyStore");
                gen.init(spec);
                gen.generateKey();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create AES key", e);
        }
    }


    private SecretKey getKey() throws Exception {
        KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
        ks.load(null);
        return ((KeyStore.SecretKeyEntry) ks.getEntry(KEY_ALIAS, null)).getSecretKey();
    }


    private byte[] encrypt(byte[] plain) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, getKey());
        byte[] iv = cipher.getIV();
        byte[] enc = cipher.doFinal(plain);

        byte[] out = new byte[iv.length + enc.length];
        System.arraycopy(iv, 0, out, 0, iv.length);
        System.arraycopy(enc, 0, out, iv.length, enc.length);
        return out;
    }


    private byte[] decrypt(byte[] data) throws Exception {
        byte[] iv = new byte[12];
        System.arraycopy(data, 0, iv, 0, 12);

        GCMParameterSpec spec = new GCMParameterSpec(128, iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, getKey(), spec);

        return cipher.doFinal(data, 12, data.length - 12);
    }


    // -------- SAVE --------

    public void saveTargets(List<TargetHost> list) {
        try {
            JSONArray arr = new JSONArray();
            for (TargetHost t : list) {
                arr.put(targetToJson(t));
            }

            byte[] enc = encrypt(arr.toString().getBytes("UTF-8"));

            File f = new File(context.getFilesDir(), FILE_NAME);
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(enc);
            fos.close();

        } catch (Exception e) {
            throw new RuntimeException("Failed to save encrypted data", e);
        }
    }


    // -------- LOAD --------

    public List<TargetHost> loadTargets() {
        try {
            File f = new File(context.getFilesDir(), FILE_NAME);
            if (!f.exists()) return new ArrayList<>();

            FileInputStream fis = new FileInputStream(f);
            byte[] data = new byte[(int) f.length()];
            fis.read(data);
            fis.close();

            String json = new String(decrypt(data), "UTF-8");

            JSONArray arr = new JSONArray(json);
            List<TargetHost> list = new ArrayList<>();

            for (int i = 0; i < arr.length(); i++) {
                list.add(jsonToTarget(arr.getJSONObject(i)));
            }

            return list;

        } catch (Exception e) {
            return new ArrayList<>();
        }
    }


    // -------- JSON helpers --------

    private JSONObject targetToJson(TargetHost t) {
        JSONObject o = new JSONObject();
        try {
            o.put("id", t.getId());
            o.put("label", t.getLabel());
            o.put("host", t.getHost());
            o.put("port", t.getPort());
            o.put("user", t.getUser());
            o.put("useSshPassword", t.isUseSshPassword());
            o.put("sshPassword", t.getSshPassword());
            o.put("useSshKey", t.isUseSshKey());
            o.put("sshPrivateKey", t.getSshPrivateKey());
            o.put("sshPrivateKeyPassword", t.getSshPrivateKeyPassword());
            o.put("sendCryptrootUnlock", t.isSendCryptrootUnlock());
            o.put("cryptrootCommand", t.getCryptrootCommand());
            o.put("luksPassword", t.getLuksPassword());
        } catch (Exception ignored) {
        }
        return o;
    }

    private TargetHost jsonToTarget(JSONObject o) {
        return new TargetHost(
                o.optLong("id"),
                o.optString("label"),
                o.optString("host"),
                o.optInt("port"),
                o.optString("user"),
                o.optBoolean("useSshPassword"),
                o.optString("sshPassword"),
                o.optBoolean("useSshKey"),
                o.optString("sshPrivateKey"),
                o.optString("sshPrivateKeyPassword"),
                o.optBoolean("sendCryptrootUnlock"),
                o.optString("cryptrootCommand"),
                o.optString("luksPassword")
        );
    }


    // -------- other helpers --------

    public long generateNewId(List<TargetHost> list) {
        long max = 0;
        for (TargetHost t : list) {
            if (t.getId() > max) max = t.getId();
        }
        return max + 1;
    }

    public TargetHost getTargetById(long id) {
        for (TargetHost t : loadTargets()) {
            if (t.getId() == id) return t;
        }
        return null;
    }

    public void replaceTarget(TargetHost updated) {
        List<TargetHost> list = loadTargets();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId() == updated.getId()) {
                list.set(i, updated);
                saveTargets(list);
                return;
            }
        }
    }
}
