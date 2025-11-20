package com.vonluehmann.unbear;
public class TargetHost {

    private final long id;
    private final String label;
    private final String host;
    private final int port;
    private final String user;

    private final boolean useSshPassword;
    private final String sshPassword;

    private final boolean useSshKey;
    private final String sshPrivateKey;
    private final String sshPrivateKeyPassword;

    private final boolean sendCryptrootUnlock;
    private final String cryptrootCommand;

    private final String luksPassword;

    public TargetHost(
            long id,
            String label,
            String host,
            int port,
            String user,
            boolean useSshPassword,
            String sshPassword,
            boolean useSshKey,
            String sshPrivateKey,
            String sshPrivateKeyPassword,
            boolean sendCryptrootUnlock,
            String cryptrootCommand,
            String luksPassword) {

        this.id = id;
        this.label = label;
        this.host = host;
        this.port = port;
        this.user = user;

        this.useSshPassword = useSshPassword;
        this.sshPassword = sshPassword;

        this.useSshKey = useSshKey;
        this.sshPrivateKey = sshPrivateKey;
        this.sshPrivateKeyPassword = sshPrivateKeyPassword;

        this.sendCryptrootUnlock = sendCryptrootUnlock;
        this.cryptrootCommand = cryptrootCommand;
        this.luksPassword = luksPassword;
    }

    public long getId() { return id; }
    public String getLabel() { return label; }
    public String getHost() { return host; }
    public int getPort() { return port; }
    public String getUser() { return user; }

    public boolean isUseSshPassword() { return useSshPassword; }
    public String getSshPassword() { return sshPassword; }

    public boolean isUseSshKey() { return useSshKey; }
    public String getSshPrivateKey() { return sshPrivateKey; }
    public String getSshPrivateKeyPassword() { return sshPrivateKeyPassword; }

    public boolean isSendCryptrootUnlock() { return sendCryptrootUnlock; }
    public String getCryptrootCommand() { return cryptrootCommand; }

    public String getLuksPassword() { return luksPassword; }

    public static class Builder {

        private long id = 0;
        private String label = "";
        private String host = "";
        private int port = 22;
        private String user = "root";

        private boolean useSshPassword = false;
        private String sshPassword = "";

        private boolean useSshKey = false;
        private String sshPrivateKey = "";
        private String sshPrivateKeyPassword = "";

        private boolean sendCryptrootUnlock = false;
        private String cryptrootCommand = "cryptroot-unlock";

        private String luksPassword = "";

        public Builder setId(long id) {
            this.id = id;
            return this;
        }

        public Builder setLabel(String label) {
            this.label = label;
            return this;
        }

        public Builder setHost(String host) {
            this.host = host;
            return this;
        }

        public Builder setPort(int port) {
            this.port = port;
            return this;
        }

        public Builder setUser(String user) {
            this.user = user;
            return this;
        }

        public Builder useSshPassword(String sshPassword) {
            this.useSshPassword = true;
            this.sshPassword = sshPassword;
            this.useSshKey = false;
            return this;
        }

        public Builder useSshKey(String privateKey, String keyPassword) {
            this.useSshKey = true;
            this.sshPrivateKey = privateKey;
            this.sshPrivateKeyPassword = keyPassword;
            this.useSshPassword = false;
            return this;
        }

        public Builder setSendCryptrootUnlock(boolean send) {
            this.sendCryptrootUnlock = send;
            return this;
        }

        public Builder setCryptrootCommand(String cmd) {
            this.cryptrootCommand = cmd;
            return this;
        }

        public Builder setLuksPassword(String pw) {
            this.luksPassword = pw;
            return this;
        }

        public TargetHost build() {
            return new TargetHost(
                    id,
                    label,
                    host,
                    port,
                    user,
                    useSshPassword,
                    sshPassword,
                    useSshKey,
                    sshPrivateKey,
                    sshPrivateKeyPassword,
                    sendCryptrootUnlock,
                    cryptrootCommand,
                    luksPassword
            );
        }
    }
}
