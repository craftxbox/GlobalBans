package com.craftxbox.globalbans;

import com.craftxbox.globalbans.command.CommandHandler;
import com.craftxbox.globalbans.command.CommandInterface;
import com.craftxbox.globalbans.command.user.LegalCommand;
import com.craftxbox.globalbans.listener.BotFarmChecker;
import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.EventDispatcher;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class GlobalBans {

	private static Logger mainLogger = LoggerFactory.getLogger(GlobalBans.class);

	private static Properties botProperties;
	private static DiscordClient discordClient;

	private static ConnectionFactory connectionFactory;

	public static void main(String[] args)  {
		try {
			botProperties = new Properties();
			botProperties.load(new FileInputStream(new File("config.properties")));
		} catch (IOException e) {
			mainLogger.error("Invalid or Missing Config", e);
		}

		connectionFactory = ConnectionFactories.get(ConnectionFactoryOptions.builder()
				.option(ConnectionFactoryOptions.DRIVER, "postgresql")
				.option(ConnectionFactoryOptions.HOST, botProperties.getProperty("bot.core.pgsql.host"))
				.option(ConnectionFactoryOptions.PORT, Integer.valueOf(botProperties.getProperty("bot.core.pgsql.port")))
				.option(ConnectionFactoryOptions.USER, botProperties.getProperty("bot.core.pgsql.user"))
				.option(ConnectionFactoryOptions.PASSWORD, botProperties.getProperty("bot.core.pgsql.pass"))
				.option(ConnectionFactoryOptions.DATABASE, botProperties.getProperty("bot.core.pgsql.db")).build());

		discordClient = new DiscordClientBuilder(botProperties.getProperty("bot.core.token")).build();

		EventDispatcher eventDispatcher = discordClient.getEventDispatcher();

		eventDispatcher.on(GuildCreateEvent.class).flatMap(e -> new BotFarmChecker().checkServer(e.getGuild())).subscribe();

		CommandHandler commandHandler = new CommandHandler(discordClient, botProperties.getProperty("bot.core.prefix"));
		commandHandler.registerCommand("legal", new LegalCommand());

		eventDispatcher.on(MessageCreateEvent.class).flatMap(commandHandler::handle).subscribe();

		discordClient.login().subscribe();

		// TODO Implement CLI
		while (true) {

		}
	}
}

