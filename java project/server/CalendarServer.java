package server;

import java.io.*;
import java.net.*;
import java.util.*;
import server.DBConnector;

public class CalendarServer {
    private static final List<ClientHandler> clients = Collections.synchronizedList(new ArrayList<>());

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

    static class ClientHandler implements Runnable {
        private final Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String username;
        private int companyId; // 회사 ID 추가

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
                    if (message.startsWith("LOGIN:")) {
                        handleLogin(message);
                    } else if (message.startsWith("MESSAGE:")) {
                        handleMessage(message);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                disconnect();
            }
        }

        private void handleLogin(String message) {
            String[] parts = message.substring(6).split(":");
            username = parts[0].trim();
            companyId = Integer.parseInt(parts[1].trim());
            System.out.println(username + "님이 로그인하였습니다. 회사 ID: " + companyId);
            sendUserListToAll();
        }

        private void handleMessage(String message) {
            String[] parts = message.substring(8).split(":", 3);
            if (parts.length == 3) {
                int chatRoomId = Integer.parseInt(parts[0].trim());
                String sender = username; // 클라이언트에서 보낸 sender(username)
                String chatMessage = parts[2].trim();

                // 사용자 ID를 가져옴 (name 기준)
                int senderId = DBConnector.getUserIdByName(sender);
                if (senderId == -1) {
                    System.err.println("유효하지 않은 사용자 ID: " + sender);
                    return;
                }

                // 단체 채팅과 개인 채팅을 구분
                if (chatRoomId == DBConnector.getChatRoomIdForGroupChat(companyId)) {
                    System.out.println("단체 채팅 메시지 수신: " + sender + ": " + chatMessage);
                    DBConnector.saveGroupChatMessage(chatRoomId, senderId, chatMessage); // DB에 저장
                    sendGroupMessage(chatRoomId, sender, chatMessage); // 모든 클라이언트에 전송
                } else {
                    System.out.println("개인 채팅 메시지 수신: " + sender + ": " + chatMessage);
                    DBConnector.saveMessage(chatRoomId, senderId, chatMessage); // DB에 저장
                    sendMessageToChatRoom(chatRoomId, sender, chatMessage); // 채팅방에 전송
                }
            }
        }

        private void sendUserListToAll() {
            StringBuilder userList = new StringBuilder("USERS:");
            synchronized (clients) {
                List<String> companyUsers = DBConnector.getUsersByCompany(companyId);

                for (String user : companyUsers) {
                    boolean isLoggedIn = clients.stream().anyMatch(client -> user.equals(client.username));
                    userList.append(user).append(":").append(isLoggedIn ? "online" : "offline").append(",");
                }
            }
            if (userList.charAt(userList.length() - 1) == ',') {
                userList.deleteCharAt(userList.length() - 1);
            }
            broadcast(userList.toString());
        }

        private void sendMessageToChatRoom(int chatRoomId, String sender, String message) {
            synchronized (clients) {
                for (ClientHandler client : clients) {
                    if (client.companyId == this.companyId) {
                        client.out.println("MESSAGE:" + chatRoomId + ":" + sender + ":" + message);
                    }
                }
                System.out.println("채팅방 " + chatRoomId + "로 메시지 전송: " + sender + ": " + message);
            }

            // 메시지를 DB에 저장
            int senderId = DBConnector.getUserIdByUsername(sender);
            if (senderId != -1) {
                DBConnector.saveMessage(chatRoomId, senderId, message);
            }
        }

        private void sendGroupMessage(int chatRoomId, String sender, String message) {
            synchronized (clients) {
                for (ClientHandler client : clients) {
                    if (client.companyId == this.companyId) {
                        client.out.println("GROUP_MESSAGE:" + sender + ":" + message);
                    }
                }
                System.out.println("단체 채팅방 " + chatRoomId + "로 메시지 전송: " + sender + ": " + message);
            }

            // 메시지를 DB에 저장
            int senderId = DBConnector.getUserIdByUsername(sender);
            if (senderId != -1) {
                DBConnector.saveMessage(chatRoomId, senderId, message);
            }
        }

        private void broadcast(String message) {
            synchronized (clients) {
                for (ClientHandler client : clients) {
                    if (client.companyId == this.companyId) {
                        client.out.println(message);
                    }
                }
            }
        }

        private void disconnect() {
            try {
                synchronized (clients) {
                    clients.remove(this);
                }
                socket.close();
                System.out.println(username + " 연결이 해제되었습니다.");
                sendUserListToAll();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
