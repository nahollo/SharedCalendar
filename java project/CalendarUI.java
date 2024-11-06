
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

    private JPanel chatArea;
    private JTextField chatInput;
    private JScrollPane chatScrollPane;

    public CalendarUI(CalendarClient client) {
        this.client = client;
        calendar = new GregorianCalendar();

        setTitle("공유 캘린더 및 채팅");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel calendarPanel = createCalendarPanel();
        add(calendarPanel, BorderLayout.CENTER);

        JPanel chatPanel = createChatPanel();
        add(chatPanel, BorderLayout.EAST);

        new Thread(new IncomingMessageHandler()).start();

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
                dayButtons[i][j].setFont(new Font("Arial", Font.PLAIN, 12));
                dayButtons[i][j].setEnabled(false);
                dayButtons[i][j].addActionListener(new DayButtonListener());
                daysPanel.add(dayButtons[i][j]);
            }
        }

        panel.add(daysPanel, BorderLayout.CENTER);
        updateCalendar();

        return panel;
    }

    private JPanel createChatPanel() {
        JPanel chatPanel = new JPanel(new BorderLayout());
        chatPanel.setPreferredSize(new Dimension(250, 400));

        // 채팅 메시지 표시 영역
        chatArea = new JPanel();
        chatArea.setLayout(new BoxLayout(chatArea, BoxLayout.Y_AXIS));
        chatScrollPane = new JScrollPane(chatArea);
        chatPanel.add(chatScrollPane, BorderLayout.CENTER);

        // 채팅 입력 필드
        chatInput = new JTextField();
        chatPanel.add(chatInput, BorderLayout.SOUTH);

        // 엔터 키로 채팅 메시지 전송
        chatInput.addActionListener(e -> {
            String message = chatInput.getText();
            if (!message.isEmpty()) {
                client.sendMessage(message);
                addChatMessage(message, true);
                chatInput.setText("");
            }
        });

        return chatPanel;
    }

    public void addChatMessage(String message, boolean isMine) {
        JPanel messagePanel = new JPanel();
        messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.X_AXIS));

        JLabel messageLabel = new JLabel(message);
        messageLabel.setOpaque(true);
        messageLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        if (isMine) {
            messageLabel.setBackground(Color.YELLOW);
            messagePanel.add(Box.createHorizontalGlue());
            messagePanel.add(messageLabel);
        } else {
            messageLabel.setBackground(Color.LIGHT_GRAY);
            messagePanel.add(messageLabel);
            messagePanel.add(Box.createHorizontalGlue());
        }

        chatArea.add(messagePanel);
        chatArea.revalidate();
        chatArea.repaint();

        SwingUtilities.invokeLater(() -> {
            JScrollBar vertical = chatScrollPane.getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        });
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

    private class DayButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            JButton button = (JButton) e.getSource();
            String day = button.getText();
            if (!day.isEmpty()) {
                String selectedDate = monthLabel.getText() + " " + day + "일";
                JOptionPane.showMessageDialog(CalendarUI.this, "선택한 날짜: " + selectedDate);
            }
        }
    }

    private class IncomingMessageHandler implements Runnable {
        @Override
        public void run() {
            while (true) {
                String message = client.receiveMessage();
                if (message != null) {
                    addChatMessage(message, false);
                }
            }
        }
    }
}
