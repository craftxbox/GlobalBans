package com.craftxbox.globalbans.listener;

import com.craftxbox.globalbans.data.GuildConfig;
import com.craftxbox.globalbans.util.DatabaseUtil;
import discord4j.core.event.domain.guild.GuildDeleteEvent;
import discord4j.core.object.audit.ActionType;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.Snowflake;
import discord4j.rest.http.client.ClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Optional;

public class ServerEvents {

    private Logger serverEventsLogger = LoggerFactory.getLogger(ServerEvents.class);

    public Mono<?> onCreate(Guild guild) {
        // I'm not sure how this is reliable but worked decently when testing
        Optional<Snowflake> selfId = guild.getClient().getSelfId();

        if (selfId.isPresent()) {
            return DatabaseUtil.getGuildConfig(guild.getId(), false)
                    .onErrorResume(t -> t instanceof DatabaseUtil.NoSuchGuildConfigException,
                            t -> guild.getMemberById(selfId.get())
                        .flatMap(Member::getBasePermissions)
                        .filter(permissions -> permissions.contains(Permission.VIEW_AUDIT_LOG))
                        .switchIfEmpty(newGuildConfig(guild).then(Mono.empty()))
                        .flatMapMany(ignored -> guild.getAuditLog(spec -> spec.setActionType(ActionType.BOT_ADD)))
                        .filter(auditLogEntry -> auditLogEntry.getTargetId().get().equals(selfId.get()))
                        .switchIfEmpty(newGuildConfig(guild).then(Mono.empty()))
                        .next()
                        .flatMap(auditLogEntry -> guild.getClient().getUserById(auditLogEntry.getResponsibleUserId()))
                        .flatMap(User::getPrivateChannel)
                        .flatMap(privateChannel -> privateChannel.createMessage(spec -> spec.setContent(
                                "To set the channel you want this bot to notify to, run b;setnotifychannel in the channel you want to set it to."
                                        + "\nRun b;togglewarnonly to toggle if you want the bot to auto-ban or not."
                                        + "\nUse b;whitelist <User ID> to whitelist already globalbanned users from the auto ban."
                                        + "\nUse b;report <User ID> <Reason + Proof> to report users to be reviewed."
                                        + "\nCriteria for reporting is if they 1: Advertise 2: Spam 3: Raid 4: Harrass. Please include proof."
                                        + "\nUse b;userinfo <Mention or ID> to get information about a specific user."
                                        + "\n\n**NOTE**: GlobalBans will not function until a notification channel is set."
                        )).onErrorResume(t2 -> t2 instanceof ClientException, t2 -> Mono.empty())
                                .and(newGuildConfig(guild)).then().cast(GuildConfig.class)));
        }

        return Mono.empty();
    }

    private Mono<GuildConfig> newGuildConfig(Guild guild) {
        return DatabaseUtil.submitConfig(guild.getId(), new GuildConfig(Instant.now(), "not_set", false));
    }

    public Mono<?> onDelete(GuildDeleteEvent guildDeleteEvent) {
        serverEventsLogger.info("Delete Guild - {} (Unavailable: {})",
                guildDeleteEvent.getGuildId().asString(), guildDeleteEvent.isUnavailable());

        if (!guildDeleteEvent.isUnavailable()) {
            return DatabaseUtil.deleteConfig(guildDeleteEvent.getGuildId());
        }

        return Mono.empty();
    }
}
