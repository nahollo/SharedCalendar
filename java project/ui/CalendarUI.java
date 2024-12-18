package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import server.DBConnector;

public class CalendarUI extends JFrame {
    private JLabel monthLabel;
    private JButton[][] dayButtons;
    private Calendar calendar;
    private JPanel chatAreaPanel; // 메시지 영역
    private JTextField chatInput;
    private JList<String> userList;
    private DefaultListModel<String> userModel;
    private JLabel chatLabel; // "~~와의 채팅" 라벨
    private PrintWriter out;
    private BufferedReader in;
    private String username;
    private String company;
    private int userId; // 로그인한 사용자 ID
    private int companyId;
    private int selectedUserId = -1; // 선택한 사용자 ID
    private int chatRoomId = 0; // 채팅방 ID
    private Socket socket;
    private JScrollPane chatScrollPane;

    public CalendarUI(String username, String company, int userId, int companyId) {
        this.username = username;
        this.company = company;
        this.userId = userId;
        this.companyId = companyId;
        calendar = new GregorianCalendar();

        setTitle(company + " - 공유 캘린더 (" + username + ")");
        setSize(1000, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // 캘린더 패널 추가
        JPanel calendarPanel = createCalendarPanel();
        add(calendarPanel, BorderLayout.CENTER);

        // 오른쪽 패널 추가 (사용자 리스트 + 채팅)
        JPanel rightPanel = new JPanel(new BorderLayout());
        JPanel userPanel = createUserListPanel();
        JPanel chatPanel = createChatPanel();

        rightPanel.add(userPanel, BorderLayout.NORTH);
        rightPanel.add(chatPanel, BorderLayout.CENTER);

        add(rightPanel, BorderLayout.EAST);

        connectToServer();
        loadChatHistory(); // 초기 단체 채팅방 메시지 로드
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JPanel createCalendarPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JPanel topPanel = new JPanel();
        JButton prevButton = new JButton("이전 달");
        JButton nextButton = new JButton("다음 달");
        monthLabel = new JLabel();

        prevButton.addActionListener(e -> {
            calendar.add(Calendar.MONTH, -1);
            updateCalendar();
        });

        nextButton.addActionListener(e -> {
            calendar.add(Calendar.MONTH, 1);
            updateCalendar();
        });

        topPanel.add(prevButton);
        topPanel.add(monthLabel);
        topPanel.add(nextButton);
        panel.add(topPanel, BorderLayout.NORTH);

        JPanel daysPanel = new JPanel(new GridLayout(7, 7));
        String[] days = { "일", "월", "화", "수", "목", "금", "토" };

        for (String day : days) {
            daysPanel.add(new JLabel(day, SwingConstants.CENTER));
        }

        dayButtons = new JButton[6][7];
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 7; j++) {
                dayButtons[i][j] = new JButton();
                dayButtons[i][j].setFont(new Font("Arial", Font.PLAIN, 10));
                dayButtons[i][j].setEnabled(false);
                daysPanel.add(dayButtons[i][j]);
            }
        }

        panel.add(daysPanel, BorderLayout.CENTER);
        updateCalendar();

        return panel;
    }

    private void updateCalendar() {
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 7; j++) {
                dayButtons[i][j].setText("");
                dayButtons[i][j].setEnabled(false);
            }
        }
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        int firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1;
        int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        monthLabel.setText(calendar.get(Calendar.YEAR) + "년 " + (calendar.get(Calendar.MONTH) + 1) + "월");

        int day = 1;
        for (int i = firstDayOfWeek; day <= daysInMonth; i++) {
            int row = i / 7;
            int col = i % 7;
            if (row < 6 && col < 7) {
                dayButtons[row][col].setText(String.valueOf(day));
                dayButtons[row][col].setEnabled(true);
                day++;
            }
        }
    }

    private JPanel createUserListPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel userLabel = new JLabel("사용자 목록");
        userLabel.setHorizontalAlignment(SwingConstants.CENTER);
        userLabel.setFont(new Font("맑은 고딕", Font.BOLD, 14));

        userModel = new DefaultListModel<>();
        userList = new JList<>(userModel);
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        userList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                    boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected,
                        cellHasFocus);
                label.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
                String item = value.toString();
                if (item.endsWith("[offline]")) {
                    label.setForeground(Color.GRAY);
                } else if (item.endsWith("[online]")) {
                    label.setForeground(Color.BLACK);
                }
                return label;
            }
        });

        userModel.addElement("[" + company + "]"); // 단체 채팅방 추가

        userList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedUser = userList.getSelectedValue();
                if (selectedUser != null) {
                    if (selectedUser.equals("[" + company + "]")) { // 단체 채팅방 선택
                        selectedUserId = -1;
                        chatRoomId = DBConnector.getChatRoomIdForGroupChat(companyId); // 단체 채팅방 ID 가져오기
                        chatLabel.setText("단체 채팅방");
                        loadChatHistory();
                    } else {
                        String usernameWithoutStatus = selectedUser.replaceAll("\\s\\[.*\\]$", "");
                        selectedUserId = DBConnector.getUserIdByname(usernameWithoutStatus, companyId);
                        chatRoomId = DBConnector.getChatRoomId(
                                Math.min(userId, selectedUserId),
                                Math.max(userId, selectedUserId));
                        if (chatRoomId == -1) {
                            chatRoomId = DBConnector.createChatRoom(
                                    Math.min(userId, selectedUserId),
                                    Math.max(userId, selectedUserId),
                                    false,
                                    companyId);
                        }
                        chatLabel.setText(usernameWithoutStatus + "와의 채팅");
                        loadChatHistory();
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(userList);

        chatLabel = new JLabel("대화 상대를 선택하세요.", SwingConstants.CENTER);
        chatLabel.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        chatLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        panel.add(userLabel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(chatLabel, BorderLayout.SOUTH);
        panel.setPreferredSize(new Dimension(250, 300));

        return panel;
    }

    private JPanel createChatPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(250, 0));

        chatAreaPanel = new JPanel();
        chatAreaPanel.setLayout(new BoxLayout(chatAreaPanel, BoxLayout.Y_AXIS)); // 수직 레이아웃
        chatAreaPanel.setBackground(Color.WHITE);

        chatScrollPane = new JScrollPane(chatAreaPanel);
        chatScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        chatInput = new JTextField();
        chatInput.addActionListener(e -> {
            String message = chatInput.getText();

            if (!message.isEmpty()) {
                if (selectedUserId == -1 && chatRoomId > 0) { // 단체 채팅방
                    out.println("MESSAGE:" + chatRoomId + ":" + username + ":" + message);
                } else if (chatRoomId > 0) { // 개인 채팅방
                    out.println("MESSAGE:" + chatRoomId + ":" + username + ":" + message);
                }
                addMyMessage(message, getCurrentTime());
                chatInput.setText("");
            }
        });

        panel.add(chatScrollPane, BorderLayout.CENTER);
        panel.add(chatInput, BorderLayout.SOUTH);

        return panel;
    }

    private void connectToServer() {
        try {
            socket = new Socket("localhost", 7890); // 서버 연결 시도
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            new Thread(this::receiveMessages).start();
            out.println("LOGIN:" + username + ":" + companyId); // 로그인 메시지 전송

            // 서버에 사용자 목록 요청
            out.println("REQUEST_USERS");
        } catch (IOException e) {
            System.err.println("서버에 연결할 수 없습니다.");
            e.printStackTrace();
        }
    }

    private void receiveMessages() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                if (message.startsWith("USERS:")) {
                    updateUserList(message.substring(6)); // 사용자 목록 업데이트
                } else if (message.startsWith("MESSAGE:")) {
                    String[] parts = message.split(":", 4);
                    int receivedChatRoomId = Integer.parseInt(parts[1]);
                    String sender = parts[2];
                    String chatMessage = parts[3];

                    if (!sender.equals(username) && receivedChatRoomId == chatRoomId) { // 내가 보낸 메시지는 제외
                        SwingUtilities.invokeLater(() -> addOtherMessage(sender, chatMessage, getCurrentTime()));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateUserList(String userListMessage) {
        SwingUtilities.invokeLater(() -> {
            userModel.clear();
            userModel.addElement("[" + company + "]"); // 단체 채팅방 추가

            List<String> onlineUsers = new ArrayList<>();
            List<String> offlineUsers = new ArrayList<>();
            String[] users = userListMessage.split(",");
            for (String user : users) {
                String[] parts = user.split(":");
                String username = parts[0];
                boolean isOnline = parts[1].equals("online");

                if (!username.equals(this.username)) { // 자신은 목록에 표시하지 않음
                    if (isOnline) {
                        onlineUsers.add(username + " [online]");
                    } else {
                        offlineUsers.add(username + " [offline]");
                    }
                }
            }

            // 정렬 후 목록 추가
            onlineUsers.sort(String::compareTo);
            offlineUsers.sort(String::compareTo);
            onlineUsers.forEach(userModel::addElement);
            offlineUsers.forEach(userModel::addElement);
        });
    }

    private void loadChatHistory() {
        chatAreaPanel.removeAll();

        List<String> messages = DBConnector.getChatHistory(chatRoomId);

        for (String message : messages) {
            String[] parts = message.split("\\|\\|");
            String sender = parts[0];
            String content = parts[1];
            String timestamp = parts[2];

            if (sender.equals(username)) {
                addMyMessage(content, timestamp);
            } else {
                addOtherMessage(sender, content, timestamp);
            }
        }

        chatAreaPanel.revalidate();
        scrollToBottom();
    }

    private void addMyMessage(String message, String timestamp) {
        JPanel bubble = createBubble(message + " [" + timestamp + "]", new Color(220, 248, 198), FlowLayout.RIGHT);
        chatAreaPanel.add(bubble);
        chatAreaPanel.revalidate();
        scrollToBottom();
    }

    private void addOtherMessage(String sender, String message, String timestamp) {
        JPanel bubble = createBubble(sender + ": " + message + " [" + timestamp + "]", Color.WHITE, FlowLayout.LEFT);
        chatAreaPanel.add(bubble);
        chatAreaPanel.revalidate();
        scrollToBottom();
    }

    private JPanel createBubble(String message, Color bgColor, int alignment) {
        JPanel bubble = new JPanel();
        bubble.setLayout(new BorderLayout());
        bubble.setBackground(bgColor);
        bubble.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel messageLabel = new JLabel("<html><body style='width: 200px'>" + message + "</body></html>");
        bubble.add(messageLabel, BorderLayout.CENTER);

        JPanel container = new JPanel();
        container.setLayout(new FlowLayout(alignment));
        container.add(bubble);

        return container;
    }

    private void scrollToBottom() {
        SwingUtilities.invokeLater(() -> chatScrollPane.getVerticalScrollBar()
                .setValue(chatScrollPane.getVerticalScrollBar().getMaximum()));
    }

    private String getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm");
        return sdf.format(new Date());
    }

    public static void main(String[] args) {
        new LoginUI();
    }
}
