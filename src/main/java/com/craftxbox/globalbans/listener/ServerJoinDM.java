package com.craftxbox.globalbans.listener;

import discord4j.core.object.audit.ActionType;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.Snowflake;
import reactor.core.publisher.Mono;

import java.util.Optional;

public class ServerJoinDM {

    public Mono<?> onJoin(Guild guild) {
        // I'm not sure how this is reliable but worked decently when testing
        Optional<Snowflake> selfId = guild.getClient().getSelfId();

        if (selfId.isPresent()) {
            return guild.getMemberById(selfId.get())
                    .flatMap(Member::getBasePermissions)
                    .filter(permissions -> permissions.contains(Permission.VIEW_AUDIT_LOG))
                    .flatMapMany(ignored -> guild.getAuditLog(spec -> spec.setActionType(ActionType.BOT_ADD)))
                    .filter(auditLogEntry -> auditLogEntry.getTargetId().get().equals(selfId.get()))
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
                    ))).onErrorContinue((__, ___) -> {});
        }

        return Mono.empty();
    }

}
