package com.craftxbox.globalbans.data;

import discord4j.core.object.util.Snowflake;

public class ReportInfo {

    private int id;
    private Snowflake reporterId;
    private Snowflake reportedId;
    private String message;
    private String attachments;
    private Snowflake respondedBy;
    private ReportStatus status;

    public ReportInfo(int id, Snowflake reporterId, Snowflake reportedId, String message,
                      String attachments, Snowflake respondedBy, ReportStatus status) {
        this.id = id;
        this.reporterId = reporterId;
        this.reportedId = reportedId;
        this.message = message;
        this.attachments = attachments;
        this.respondedBy = respondedBy;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public Snowflake getReporterId() {
        return reporterId;
    }

    public Snowflake getReportedId() {
        return reportedId;
    }

    public String getMessage() {
        return message;
    }

    public String getAttachments() {
        return attachments;
    }

    public Snowflake getRespondedBy() {
        return respondedBy;
    }

    public void setRespondedBy(Snowflake respondedBy) {
        this.respondedBy = respondedBy;
    }

    public ReportStatus getStatus() {
        return status;
    }

    public void setStatus(ReportStatus status) {
        this.status = status;
    }

    public enum ReportStatus {
        PENDING,
        UNDER_REVIEW,
        ACCEPTED,
        REJECTED
    }

    @Override
    public String toString() {
        return "ReportInfo{" +
                "id=" + id +
                ", reporterId=" + reporterId +
                ", reportedId=" + reportedId +
                ", message='" + message + '\'' +
                ", attachments='" + attachments + '\'' +
                ", respondedBy=" + respondedBy +
                ", status=" + status +
                '}';
    }
}
