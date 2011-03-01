package com.rackspace.idm.domain.entity;

public class EmailSettings {
    
    private int smtpPort;
    private String smtpHost;
    private String smtpUsername;
    private String smtpPassword;
    
    private boolean debug;
    private boolean useSSL;
    private boolean useTSL;
    
    public EmailSettings(int smtpPort, String smtpHost, String smtpUsername,
        String smtpPassword, boolean debug, boolean useSSL, boolean useTSL) {

        this.smtpPort = smtpPort;
        this.smtpHost = smtpHost;
        this.smtpUsername = smtpUsername;
        this.smtpPassword = smtpPassword;
        this.debug = debug;
        this.useSSL = useSSL;
        this.useTSL = useTSL;
    }
    
    public int getSmtpPort() {
        return smtpPort;
    }
    public void setSmtpPort(int smtpPort) {
        this.smtpPort = smtpPort;
    }
    public String getSmtpHost() {
        return smtpHost;
    }
    public void setSmtpHost(String smtpHost) {
        this.smtpHost = smtpHost;
    }
    public String getSmtpUsername() {
        return smtpUsername;
    }
    public void setSmtpUsername(String smtpUsername) {
        this.smtpUsername = smtpUsername;
    }
    public String getSmtpPassword() {
        return smtpPassword;
    }
    public void setSmtpPassword(String smtpPassword) {
        this.smtpPassword = smtpPassword;
    }
    public boolean isDebug() {
        return debug;
    }
    public void setDebug(boolean debug) {
        this.debug = debug;
    }
    public boolean getUseSSL() {
        return useSSL;
    }
    public void setUseSSL(boolean useSSL) {
        this.useSSL = useSSL;
    }
    public boolean getUseTSL() {
        return useTSL;
    }
    public void setUseTSL(boolean useTSL) {
        this.useTSL = useTSL;
    }
}
