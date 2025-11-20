package com.vonluehmann.unbear;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;

import java.io.*;

public class SshClient {

    private SSHClient client;

    public void connect(String host, int port) throws Exception {
        client = new SSHClient();
        client.addHostKeyVerifier(new PromiscuousVerifier());
        client.connect(host, port);
    }

    public void loginWithPassword(String user, String password) throws Exception {
        client.authPassword(user, password);
    }

    public void loginWithKey(String user, String privateKey, String keyPassword) throws Exception {
        ByteArrayInputStream keyStream =
                new ByteArrayInputStream(privateKey.getBytes());
        client.authPublickey(user, client.loadKeys(keyStream.toString(), keyPassword));
    }

    public void runCryptrootUnlock(String command,
                                   String luksPassword,
                                   SshOutputListener listener) throws Exception {

        try (Session session = client.startSession()) {

            Session.Command cmd = session.exec(command);

            // lese parallel stdout
            Thread readerThread = new Thread(() -> {
                try (BufferedReader br =
                             new BufferedReader(new InputStreamReader(cmd.getInputStream()))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        listener.onOutput(line);
                    }
                } catch (Exception ignored) {
                }
            });
            readerThread.start();

            OutputStream os = cmd.getOutputStream();
            os.write((luksPassword + "\n").getBytes());
            os.flush();

            cmd.join();

            readerThread.join();
        }
    }

    public void runDirectPasswordInteraction(String luksPassword,
                                             SshOutputListener listener) throws Exception {

        try (Session session = client.startSession()) {

            Session.Command cmd = session.exec("");

            Thread readerThread = new Thread(() -> {
                try (BufferedReader br =
                             new BufferedReader(new InputStreamReader(cmd.getInputStream()))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        listener.onOutput(line);
                    }
                } catch (Exception ignored) {
                }
            });
            readerThread.start();

            OutputStream os = cmd.getOutputStream();
            os.write((luksPassword + "\n").getBytes());
            os.flush();

            cmd.join();

            readerThread.join();
        }
    }

    public void close() {
        if (client != null) {
            try {
                client.disconnect();
            } catch (Exception ignored) {
            }
        }
    }
}
