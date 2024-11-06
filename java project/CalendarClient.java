
import java.io.*;
import java.net.Socket;

public class CalendarClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    public CalendarClient(String serverAddress, int port) throws IOException {
        socket = new Socket(serverAddress, port);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        System.out.println("서버에 연결되었습니다.");
    }

    public void register(String email, String name, String password) {
        out.println("REGISTER:" + email + "," + name + "," + password);
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

    public String receiveMessage() {
        try {
            return in.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
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
            new LoginUI(client);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
