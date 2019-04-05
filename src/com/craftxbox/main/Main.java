package com.craftxbox.main;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.ini4j.Wini;

import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventDispatcher;
import sx.blah.discord.util.DiscordException;

public class Main extends JFrame{
	
	public static IDiscordClient disClient;
	public Main() {
		setSize(340, 100);
		setResizable(false);
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		setIconImage(Toolkit.getDefaultToolkit().getImage(Main.class.getResource("globalbans.png")));
		setTitle("Globalbans Administration Panel");
		JPanel panel = new JPanel();
		getContentPane().add(panel, BorderLayout.NORTH);
		JButton btnLogs = new JButton("Logs");
		btnLogs.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				JDialog dialog = new JDialog();
				JPanel diapanel = new JPanel();
				dialog.setSize(640, 480);
				dialog.getContentPane().add(diapanel, BorderLayout.CENTER);
				dialog.setVisible(true);
			}
		});
		JButton btnRestart = new JButton("Restart");
		btnRestart.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				Wini ini;
				try {
					ini = new Wini(new File("config.ini"));
					com.craftxbox.main.Main.txtRunning.setText("Starting");
					com.craftxbox.main.Main.disClient.logout();
					com.craftxbox.main.Main.disClient = null;
					com.craftxbox.main.Main.disClient = Main.createClient(ini.get("Config", "token"), true);
			        EventDispatcher dispatcher = disClient.getDispatcher(); // Gets the EventDispatcher instance for this client instance
			        dispatcher.registerListener(new com.craftxbox.main.AnnotationListener());
				}
			    catch(Exception e){
			    	e.printStackTrace();
			    	txtRunning.setText("Errored");
			    }
			}
		});
		panel.add(btnRestart);
		panel.add(btnLogs);
		
		JButton btnExit = new JButton("Exit");
		btnExit.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				System.exit(0);
			}
		});
		panel.add(btnExit);
		
		JPanel panel_1 = new JPanel();
		getContentPane().add(panel_1, BorderLayout.SOUTH);
		
		JLabel lblStatus = new JLabel("Status:");
		panel_1.add(lblStatus);
		
		txtRunning = new JTextField();
		txtRunning.setFont(new Font("Tahoma", Font.PLAIN, 11));
		txtRunning.setColumns(4);
		txtRunning.setText("Starting");
		panel_1.add(txtRunning);
		txtRunning.setEditable(false);
		
		JLabel lblGuilds = new JLabel("Guilds:");
		panel_1.add(lblGuilds);
		
		textField = new JTextField();
		textField.setText("0");
		textField.setEditable(false);
		panel_1.add(textField);
		textField.setColumns(4);
		
		JLabel lblShards = new JLabel("Shards:");
		panel_1.add(lblShards);
		
		textField_1 = new JTextField();
		textField_1.setEditable(false);
		textField_1.setText("0");
		panel_1.add(textField_1);
		textField_1.setColumns(5);

	}
	private static final long serialVersionUID = 1L;
	public static JTextField txtRunning;
	public static JTextField textField;
	private static JTextField textField_1;
	public static void main(String[] args) {
		Main main = new Main();
		Wini ini;
		try {
			ini = new Wini(new File("config.ini"));
			try{
				System.out.println("-------------------------");
		        disClient = Main.createClient(ini.get("Config", "token"), true);
		        EventDispatcher dispatcher = disClient.getDispatcher();
		        dispatcher.registerListener(new AnnotationListener());
		        main.setVisible(true);
			}catch(DiscordException e){
				e.printStackTrace();
				txtRunning.setText("Errored");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
    public static IDiscordClient createClient(String token, boolean login) {
        ClientBuilder clientBuilder = new ClientBuilder();
        clientBuilder
	    	.withToken(token)
	    	.withRecommendedShardCount()
	    	.setMaxMessageCacheCount(0)
	    	.setMaxReconnectAttempts(100000);
        try {
            if (login) {
            	IDiscordClient client = clientBuilder.login();
            	com.craftxbox.main.Main.textField_1.setText(Integer.toString(client.getShardCount()));
                return client;
                
            } else {
                return clientBuilder.build();
            }
        } catch (DiscordException e) {
            e.printStackTrace();
            return null;
        }
    }

}

