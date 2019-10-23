package server;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.Scene;
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

public class SimpleIMServer extends Application {

	public static void main(String[] args) {
		launch(args);
	}

	BorderPane bp;
	TextArea messages;
	TextField messageWritingField;
	Button sendButton;

	Socket clientConnection;
	ObjectOutputStream output;
	ObjectInputStream input;
	Thread socketAccepter;
	boolean darkTheme = false;
	int number;

	@Override
	public void start(Stage primaryStage) throws Exception {

		bp = new BorderPane();
		messages = new TextArea();
		messages.setEditable(false);
		messages.setOpacity(0.60);
		messages.setWrapText(true);
		bp.setCenter(messages);

		HBox messageWritingArea = new HBox();
		messageWritingField = new TextField();
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
		sendButton = new Button("Send");
		Button darkMode = new Button("Dark Theme");
		Button purge = new Button("Purge");
		messageWritingArea.getChildren().add(messageWritingField);
		messageWritingArea.getChildren().add(sendButton);
		messageWritingArea.getChildren().add(darkMode);
		messageWritingArea.getChildren().add(purge);
		bp.setBottom(messageWritingArea);

		primaryStage.setTitle("SimpleIM Server");
		primaryStage.setResizable(false);
		primaryStage.setScene(new Scene(bp, 400, 400));
		primaryStage.show();
		primaryStage.setOnHidden(e -> {
			try {
				this.clientConnection.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			this.socketAccepter.interrupt();
		});

		darkMode.setOnAction((event) -> {

			if(darkTheme) {
				primaryStage.getScene().getStylesheets().remove("dark_theme.css");
				darkTheme = false;
			}
			else {
				primaryStage.getScene().getStylesheets().add("dark_theme.css");
				darkTheme = true;
			}
		});

		/* set up button handler */
		sendButton.setOnAction((event) -> {
			try {
				sendMessage(messageWritingField.getText());
				messageWritingField.setText("");
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
		
		purge.setOnAction((event) -> {
			this.messages.clear();
		});
		
		/* set up networking */
		ServerSocket serverSocket = new ServerSocket(4000);
		appendMessage("Waiting for a client connection...");

		// create a new thread to manage connections, so it doesn't block the UI
		this.socketAccepter = new Thread() {
			public void run() {
				try {
					// accept connection
					clientConnection = serverSocket.accept();
					output = new ObjectOutputStream(clientConnection.getOutputStream());
					input = new ObjectInputStream(clientConnection.getInputStream());
					Platform.runLater(() -> {
						appendMessage("connected!");
					});

					// read in messages continuously
					try {
						while (true) {
							Message received = (Message) input.readObject();
							Platform.runLater(() -> received.reverseCipher());
							Platform.runLater(() -> appendMessage("[Guest]: " + received.getMessage()));
							if(!primaryStage.isFocused()) {
								Platform.runLater(() -> primaryStage.requestFocus());
							}
						}
					} catch (SocketTimeoutException exc) {
						Platform.runLater(() -> appendMessage("Timed out while waiting for a response."));
					} catch (EOFException exc) {
						Platform.runLater(() -> appendMessage("Client disconnected....RIP"));
					} catch (IOException exc) {
						// some other I/O error: print it, log it, etc.
						exc.printStackTrace();
					} catch (ClassNotFoundException exc) {
						exc.printStackTrace();
					} finally {
						clientConnection.close();
						serverSocket.close();
					}
				} catch (IOException ioe) {
					ioe.printStackTrace();
					Platform.runLater(() -> appendMessage("An error occured while waiting for a connection."));
				}
			}
		};
		socketAccepter.start();
	}

	/**
	 * Writes a DemoMessage object to the client and updates the messages TextArea
	 * via appendMessage.
	 * 
	 * @param message the message to send to the server
	 * @throws IOException
	 */
	public void sendMessage(String message) throws IOException {
		Message newMessage = new Message(message, number++);
		newMessage.cipher();
		output.writeObject(newMessage);
		output.flush();
		appendMessage("[Me]: " + newMessage.time + " " + message);
		this.messages.appendText("");
	}

	/**
	 * Appends the given message to the end of the messages TextArea. It will
	 * automatically append a newline to the end of the given message.
	 * 
	 * @param message the message to be appended to the messages TextArea
	 */
	private void appendMessage(String message) {
		messages.setText(messages.getText() + message + '\n');
		this.messages.appendText("");
	}

}