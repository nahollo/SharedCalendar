import java.io.*;
import java.net.*;
import java.util.*;

public class CalendarServer {
    private static List<ClientHandler> clients = new ArrayList<>();
    private static Map<String, String> userDatabase = new HashMap<>();

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
                        case "REGISTER":
                            handleRegistration(content);
                            break;
                        case "LOGIN":
                            handleLogin(content);
                            break;
                        case "MESSAGE":
                            broadcastMessage(content, this);
                            break;
                        default:
                            System.out.println("알 수 없는 명령: " + command);
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

        private void handleRegistration(String content) {
            String[] data = content.split(",");
            String email = data[0].trim();
            String name = data[1].trim();
            String password = data[2].trim();

            if (userDatabase.containsKey(email)) {
                out.println("REGISTRATION_FAILED: 이미 사용 중인 이메일입니다.");
            } else {
                userDatabase.put(email, password);
                out.println("REGISTRATION_SUCCESS: 회원가입이 완료되었습니다.");
            }
        }

        private void handleLogin(String content) {
            String[] data = content.split(",");
            String email = data[0].trim();
            String password = data[1].trim();

            if (userDatabase.containsKey(email) && userDatabase.get(email).equals(password)) {
                out.println("LOGIN_SUCCESS: 로그인 성공!");
            } else {
                out.println("LOGIN_FAILED: 로그인 실패. 이메일과 비밀번호를 확인하세요.");
            }
        }

        private void broadcastMessage(String message, ClientHandler sender) {
            for (ClientHandler client : clients) {
                if (client != sender) {
                    client.out.println("MESSAGE:" + message);
                }
            }
        }
    }
}
