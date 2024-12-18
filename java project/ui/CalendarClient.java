package ui;

import java.io.*;
import java.net.Socket;

public class CalendarClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    public CalendarClient(String serverAddress, int port, String username) throws IOException {
        socket = new Socket(serverAddress, port);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        out.println("LOGIN:" + username);

        new Thread(this::receiveMessages).start();
    }

    public void sendMessage(int chatRoomId, String sender, String message) {
        out.println("MESSAGE:" + chatRoomId + ":" + sender + ":" + message);
    }

    private void receiveMessages() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                if (message.startsWith("MESSAGE:")) {
                    String[] parts = message.split(":", 4);
                    int chatRoomId = Integer.parseInt(parts[1]);
                    String sender = parts[2];
                    String chatMessage = parts[3];
                    System.out.println("[채팅방 " + chatRoomId + "] " + sender + ": " + chatMessage);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
