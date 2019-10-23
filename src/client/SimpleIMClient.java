package client;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.net.SocketTimeoutException;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import message.Message;

public class SimpleIMClient extends Application {

	BorderPane bp;
	TextArea messages;
	TextField messageWritingField;
	Button send;
	Socket serverConnection;
	ObjectOutputStream out;
	ObjectInputStream in;
	Thread socketAccepter;
	boolean darkTheme = false;
	int number;
	
	public static void main(String[] args) {
		
		launch(args);
		
	}
	
	
	@Override
	public void start(Stage stage) throws Exception {
		
		initGuiObjects(stage);
		
		appendMessage("Connecting to server...");
		
		createThread(stage);
	}
	
	private void initGuiObjects(Stage stage) {
		
		this.bp = new BorderPane();
		
		stage.setTitle("SimpleIM Client");
		stage.setResizable(false);
		stage.setScene(new Scene(bp, 400, 400));
		stage.show();
		stage.setOnCloseRequest((WindowEvent e) -> {
			this.socketAccepter.interrupt();
		});
		stage.setOnHidden(e -> {
			try {
				this.serverConnection.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			this.socketAccepter.interrupt();
		});
		
		this.messages = new TextArea();
		messages.setEditable(false);
		messages.setOpacity(0.60);
		messages.setWrapText(true);
		
		this.messageWritingField = new TextField();
		messageWritingField.setOnKeyPressed((KeyEvent k) -> {
			
			if(k.getCode().equals(KeyCode.ENTER)) {
				try {
					sendMessage(messageWritingField.getText());
					messageWritingField.setText("");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
				
		send = new Button("Send");
		send.setOnAction((event) -> {
			try {
				sendMessage(messageWritingField.getText());
				messageWritingField.setText("");
				Platform.runLater(() -> appendMessage("The connection has been lost."));
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
		
		Button darkMode = new Button("Dark Theme");
		darkMode.setOnAction((event) -> {

			if(darkTheme) {
				stage.getScene().getStylesheets().remove("dark_theme.css");
				darkTheme = false;
			}
			else {
				stage.getScene().getStylesheets().add("dark_theme.css");
				darkTheme = true;
			}
		});
		
		Button purge = new Button("Purge");
		purge.setOnAction((event) -> {
			this.messages.clear();
		});
		
		HBox hb = new HBox();
		hb.getChildren().addAll(messageWritingField, send, darkMode, purge);
		bp.setBottom(hb);
		bp.setCenter(messages);
		
	}
	
	private void createThread(Stage stage) {
		
		this.socketAccepter = new Thread() {
			
			public void run() {
				
				try {
					// accept connection
					serverConnection = new Socket("127.0.0.1", 4000);
					out = new ObjectOutputStream(serverConnection.getOutputStream());
					in = new ObjectInputStream(serverConnection.getInputStream());

					Platform.runLater(() -> {
						appendMessage("connected!");
					});
					// read in messages continuously
					try {
						while (true) {
							Message received = (Message) in.readObject();
							Platform.runLater(() -> received.reverseCipher());
							Platform.runLater(() -> appendMessage("[Guest]: " + received.getMessage()));
							if(!stage.isFocused()) {
								Platform.runLater(() -> stage.requestFocus());
							}
						}
					} catch (SocketTimeoutException exc) {
						Platform.runLater(() -> appendMessage("Timed out while waiting for a response."));
					} catch (EOFException exc) {
						Platform.runLater(() -> appendMessage("Server disconnected....RIP"));
					} catch (IOException exc) {
						exc.printStackTrace();
					} catch (ClassNotFoundException exc) {
						exc.printStackTrace();
					}
				} catch (IOException ioe) {
					ioe.printStackTrace();
					Platform.runLater(() -> appendMessage("An error occured while waiting for a connection."));
					}
				}
			};
			socketAccepter.start();
			
	}
	
	public void sendMessage(String message) throws IOException {
		Message newMessage = new Message(message, number++);
		newMessage.cipher();
		out.writeObject(newMessage);
		out.flush();
		appendMessage("[Me]: " + newMessage.time + " " + message);
		this.messages.appendText("");
	}
	
	private void appendMessage(String message) {
		messages.setText(messages.getText() + message + '\n');
		this.messages.appendText("");
	}
	
	
}