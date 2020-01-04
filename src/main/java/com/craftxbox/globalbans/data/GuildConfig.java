package com.craftxbox.globalbans.data;

import java.time.Instant;

public class GuildConfig {

    private Instant createdOn;
    private String notificationChannel;
    private boolean warnOnly;

    public GuildConfig(Instant createdOn, String notificationChannel, boolean warnOnly) {
        this.createdOn = createdOn;
        this.notificationChannel = notificationChannel;
        this.warnOnly = warnOnly;
    }

    public Instant getCreatedOn() {
        return createdOn;
    }

    public String getNotificationChannel() {
        return notificationChannel;
    }

    public boolean isWarnOnly() {
        return warnOnly;
    }
}
