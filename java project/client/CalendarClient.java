package client;

import java.io.*;
import java.net.Socket;
import java.util.*;

import ui.CalendarUI;

public class CalendarClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    public CalendarClient(String serverAddress, int port) throws IOException {
        socket = new Socket(serverAddress, port);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // 메시지 수신 스레드 실행
        new Thread(this::receiveMessages).start();
    }

    public void register(String email, String name, String password, String companyName) {
        out.println("REGISTER:" + email + "," + name + "," + password + "," + companyName);
        try {
            String response = in.readLine();
            System.out.println(response);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean login(String email, String password) {
        out.println("LOGIN:" + email + "," + password);
        try {
            String response = in.readLine();
            return response.startsWith("LOGIN_SUCCESS");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void sendMessage(String message) {
        out.println("MESSAGE:" + message);
    }

    private void receiveMessages() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                if (message.startsWith("MESSAGE:")) {
                    System.out.println(message.substring(8)); // 메시지 출력
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

    public static void main(String[] args) {
        try {
            CalendarClient client = new CalendarClient("localhost", 7890);
            new CalendarUI(client); // 캘린더 UI 실행
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
