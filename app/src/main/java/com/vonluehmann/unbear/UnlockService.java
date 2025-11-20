package com.vonluehmann.unbear;
public class UnlockService {

    public interface Callback {
        void onProgress(String msg);
        void onLog(String line);
        void onSuccess();
        void onError(String error);
    }

    public void unlock(TargetHost target, Callback cb) {

        new Thread(() -> {
            SshClient ssh = new SshClient();

            try {
                cb.onProgress("Verbinde mit " + target.getHost());
                ssh.connect(target.getHost(), target.getPort());

                if (target.isUseSshPassword()) {
                    cb.onProgress("Authentifiziere");
                    ssh.loginWithPassword(target.getUser(), target.getSshPassword());
                } else if (target.isUseSshKey()) {
                    cb.onProgress("Authentifiziere mit Key");
                    ssh.loginWithKey(target.getUser(),
                            target.getSshPrivateKey(),
                            target.getSshPrivateKeyPassword());
                } else {
                    cb.onError("Keine Auth Methode");
                    return;
                }

                cb.onProgress("Sende Passwort");

                if (target.isSendCryptrootUnlock()) {
                    ssh.runCryptrootUnlock(
                            target.getCryptrootCommand(),
                            target.getLuksPassword(),
                            line -> cb.onLog(line)
                    );
                } else {
                    ssh.runDirectPasswordInteraction(
                            target.getLuksPassword(),
                            line -> cb.onLog(line)
                    );
                }

                cb.onSuccess();

            } catch (Exception e) {
                cb.onError(e.toString());
            } finally {
                ssh.close();
            }

        }).start();
    }


}
