package ui;

import client.CalendarClient;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class CalendarUI extends JFrame {
    private JLabel monthLabel;
    private JButton[][] dayButtons;
    private Calendar calendar;
    private CalendarClient client;
    private JTextArea chatArea;
    private JTextField chatInput;

    public CalendarUI(CalendarClient client) {
        this.client = client;
        calendar = new GregorianCalendar();

        setTitle("공유 캘린더");
        setSize(800, 600); // 크기 확장
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // 캘린더 패널
        JPanel calendarPanel = createCalendarPanel();
        add(calendarPanel, BorderLayout.CENTER);

        // 채팅 패널
        JPanel chatPanel = createChatPanel();
        add(chatPanel, BorderLayout.EAST);

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

    private JPanel createChatPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(250, 0)); // 채팅 패널 크기 설정

        // 채팅 기록 표시
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(chatArea);

        // 채팅 입력
        chatInput = new JTextField();
        chatInput.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String message = chatInput.getText();
                if (!message.isEmpty()) {
                    client.sendMessage(message); // 메시지 서버로 전송
                    chatInput.setText(""); // 입력 필드 초기화
                }
            }
        });

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(chatInput, BorderLayout.SOUTH);

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

    // 채팅 메시지 수신 업데이트 메서드
    public void updateChatArea(String message) {
        chatArea.append(message + "\n");
    }
}
