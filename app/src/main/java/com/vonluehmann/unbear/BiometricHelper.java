package com.vonluehmann.unbear;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import java.util.concurrent.Executor;

public class BiometricHelper {

    public interface Callback {
        void onAuthenticated();
        void onFailed();
    }

    private final Context context;
    private final Executor executor;

    public BiometricHelper(Context context) {
        this.context = context;
        this.executor = ContextCompat.getMainExecutor(context);
    }

    public boolean canUseBiometrics() {
        BiometricManager manager = BiometricManager.from(context);
        int result = manager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG
                        | BiometricManager.Authenticators.DEVICE_CREDENTIAL
        );

        return result == BiometricManager.BIOMETRIC_SUCCESS;
    }

    public void authenticate(Callback callback) {
        BiometricPrompt prompt = new BiometricPrompt(
                (androidx.fragment.app.FragmentActivity) context,
                executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        callback.onAuthenticated();
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        callback.onFailed();
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        callback.onFailed();
                    }
                });

        BiometricPrompt.PromptInfo promptInfo =
                new BiometricPrompt.PromptInfo.Builder()
                        .setTitle("Unlock")
                        .setDescription("Authenticate to unlock your secure data")
                        .setAllowedAuthenticators(
                                BiometricManager.Authenticators.BIOMETRIC_STRONG
                                        | BiometricManager.Authenticators.DEVICE_CREDENTIAL
                        )
                        .build();

        prompt.authenticate(promptInfo);
    }
}
