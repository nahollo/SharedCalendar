package server;

import java.io.*;
import java.net.*;
import java.util.*;

public class CalendarServer {
    private static List<ClientHandler> clients = new ArrayList<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(7890)) {
            System.out.println("서버가 시작되었습니다.");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clients.add(clientHandler);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String userEmail;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                String message;
                while ((message = in.readLine()) != null) {
                    String[] parts = message.split(":", 2);
                    String command = parts[0];
                    String content = parts.length > 1 ? parts[1] : "";

                    switch (command) {
                        case "MESSAGE":
                            broadcastMessage(content, this);
                            break;
                        case "ADD_SCHEDULE":
                            handleAddSchedule(content);
                            break;
                        case "GET_SCHEDULES":
                            handleGetSchedules(content);
                            break;
                        default:
                            out.println("ERROR: 알 수 없는 명령어입니다.");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void broadcastMessage(String message, ClientHandler sender) {
            for (ClientHandler client : clients) {
                if (client != sender) {
                    client.out.println("MESSAGE:" + sender.userEmail + ": " + message);
                }
            }
        }

        private void handleAddSchedule(String content) {
            // 캘린더 일정 추가 기능 (DB와 연결 해제)
            String[] data = content.split(",");
            if (data.length >= 3) {
                String startDate = data[0].trim();
                String endDate = data[1].trim();
                String title = data[2].trim();

                // 단순 캘린더 데이터 출력
                System.out.println("새 일정 추가:");
                System.out.println("시작일: " + startDate + ", 종료일: " + endDate + ", 제목: " + title);

                out.println("SCHEDULE_ADDED");
            } else {
                out.println("ERROR: Invalid data format.");
            }
        }

        private void handleGetSchedules(String content) {
            // 캘린더 일정 조회 기능 (DB와 연결 해제)
            String date = content.trim();

            // 임시 일정 데이터
            List<String> schedules = Arrays.asList(
                    "회의 (" + date + " 10:00 ~ 12:00)",
                    "개발 리뷰 (" + date + " 14:00 ~ 15:00)");

            if (schedules.isEmpty()) {
                out.println("SCHEDULES:일정이 없습니다;");
            } else {
                out.println("SCHEDULES:" + String.join(";", schedules));
            }
        }
    }
}
