package com.craftxbox.main;

import java.io.File;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextPane;

import org.ini4j.InvalidFileFormatException;
import org.ini4j.Wini;

import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.presence.Activity;
import discord4j.core.object.presence.Presence;
import discord4j.core.object.util.Snowflake;

public class Main extends JFrame{
	public Main() {
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		setTitle("Globalbans Administration Panel");
		getContentPane().setLayout(null);
		setBounds(300, 300, 320, 130);
		setResizable(false);
		
		JButton btnRestart = new JButton("Restart");
		btnRestart.setBounds(10, 11, 89, 23);
		getContentPane().add(btnRestart);
		
		JButton btnLogs = new JButton("Logs");
		btnLogs.setBounds(109, 11, 89, 23);
		getContentPane().add(btnLogs);
		
		JButton btnExit = new JButton("Exit");
		btnExit.setBounds(208, 11, 89, 23);
		getContentPane().add(btnExit);
		
		JTextPane serverStatus = new JTextPane();
		serverStatus.setText("Unconnected");
		serverStatus.setBounds(10, 45, 89, 20);
		getContentPane().add(serverStatus);
		
		JTextPane connectionStatus = new JTextPane();
		connectionStatus.setText("Disconnected");
		connectionStatus.setBounds(208, 45, 89, 20);
		getContentPane().add(connectionStatus);
		
		JTextPane errors = new JTextPane();
		errors.setText("No Errors");
		errors.setBounds(109, 45, 89, 20);
		errors.setEditable(false);
		getContentPane().add(errors);
		
		JLabel lblNewLabel = new JLabel("Servers");
		lblNewLabel.setBounds(10, 68, 89, 14);
		getContentPane().add(lblNewLabel);
		
		JLabel lblErrors = new JLabel("Errors");
		lblErrors.setBounds(109, 68, 89, 14);
		getContentPane().add(lblErrors);
		
		JLabel lblStatus = new JLabel("Status");
		lblStatus.setBounds(208, 68, 89, 14);
		getContentPane().add(lblStatus);
	}
	
	static DiscordClient client;
	static AnnotationListener listener;
	private static String instance = Double.toString(Math.random());
	
	public static void main(String[] args) throws InvalidFileFormatException, IOException {
		Main main = new Main();
		main.setVisible(true);
		Wini config = new Wini(new File("config.ini"));
		startClient(config.get("config", "token"));
	}
	
	public static void startClient(String token) {
		client = new DiscordClientBuilder(token).build();
		client.getEventDispatcher().on(MessageCreateEvent.class).subscribe(event ->{
			listener.onMessageReceive(event);
		});
		client.getEventDispatcher().on(ReadyEvent.class).subscribe(event ->{
			client.updatePresence(Presence.online(Activity.watching(client.getGuilds().count().block()+" Servers | b;help")));
			client.getChannelById(Snowflake.of("322462068475428864")).subscribe(chan ->{
				TextChannel tchan = (TextChannel) chan;
				tchan.createMessage("b;kill "+instance);
			});
		});
	}
	
	public static void stopClient() {
		client.logout().block();
		client = null;
	}
}

