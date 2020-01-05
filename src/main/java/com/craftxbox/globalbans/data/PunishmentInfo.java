package com.craftxbox.globalbans.data;

import discord4j.core.object.util.Snowflake;

import java.time.Instant;

public class PunishmentInfo {

    private Snowflake userId;
    private Snowflake issuedBy;
    private CaseType caseType;
    private int caseId;
    private PunishmentType punishmentType;
    private Instant punishmentTime;
    private Instant punishmentExpiry;
    private String reason;

    public PunishmentInfo(Snowflake userId, Snowflake issuedBy, CaseType caseType, int caseId,
                          PunishmentType punishmentType, Instant punishmentTime,
                          Instant punishmentExpiry, String reason) {
        this.userId = userId;
        this.issuedBy = issuedBy;
        this.caseType = caseType;
        this.caseId = caseId;
        this.punishmentType = punishmentType;
        this.punishmentTime = punishmentTime;
        this.punishmentExpiry = punishmentExpiry;
        this.reason = reason;
    }

    public enum CaseType {
        LOCAL,
        GLOBAL
    }

    public enum PunishmentType {
        WARN,
        BAN
    }
}
