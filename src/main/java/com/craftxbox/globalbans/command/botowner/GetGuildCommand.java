package com.craftxbox.globalbans.command.botowner;

import com.craftxbox.globalbans.GlobalBans;
import com.craftxbox.globalbans.command.CommandInterface;
import com.craftxbox.globalbans.command.Describe;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Image;
import discord4j.core.object.util.Snowflake;
import discord4j.rest.http.client.ClientException;
import reactor.core.publisher.Mono;

import java.text.DecimalFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Describe(botOwner = true)
public class GetGuildCommand implements CommandInterface {

    private DecimalFormat decimalFormat = new DecimalFormat("#.##");
    private Pattern GUILD_REGEX = Pattern.compile("\\d{17,21}");

    @Override
    public Mono<Message> handleCommand(Member member, Message message, TextChannel channel, String[] args) {
        Snowflake mentionedGuild = null;

        for (String s : args) {
            Matcher matcher = GUILD_REGEX.matcher(s);

            if (matcher.matches()) {
                mentionedGuild = Snowflake.of(matcher.group());
                break;
            }
        }

        if (mentionedGuild != null) {
            return channel.getClient().getGuildById(mentionedGuild)
                    .flatMap(guild -> Mono.just(guild.getMemberCount().orElse(1))
                        .flatMap(memberCount -> guild.getMembers()
                            .filter(User::isBot).count()
                                .flatMap(trueMemberCount -> Mono.just((trueMemberCount / (double) memberCount) * 100)
                                    .flatMap(botPercentage -> guild.getOwner()
                                        .flatMap(guildOwner -> channel.createEmbed(spec -> {
                                            spec.setAuthor(guild.getName(), "", guild.getIconUrl(Image.Format.PNG)
                                                    .orElse(""));
                                            spec.addField("Bots", String.valueOf(memberCount - trueMemberCount), true);
                                            spec.addField("Humans", String.valueOf(trueMemberCount), true);
                                            spec.addField("Total Users", String.valueOf(memberCount), true);
                                            spec.addField("Bot Percentage",  decimalFormat.format(botPercentage) + "%", true);
                                            spec.addField("Owner", guildOwner.getDisplayName()
                                                    + "#" + guildOwner.getDiscriminator()
                                                    + " (" + guildOwner.getId().asString() + ")", true);
                                        }))))))
                    .onErrorResume(t -> t instanceof ClientException,
                            t -> channel.createMessage(spec -> spec.setContent(String.format("%s No valid guilds were specified.",
                                    GlobalBans.getConfigurationValue("bot.core.emote.cross")))));
        }

        return channel.createMessage(spec -> spec.setContent(String.format("%s No valid guilds were specified.",
                GlobalBans.getConfigurationValue("bot.core.emote.cross"))));
    }
}
