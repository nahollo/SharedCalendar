package ui;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import server.DBConnector;

public class LoginUI {
    public LoginUI() {
        JFrame frame = new JFrame("로그인");
        frame.setSize(300, 200);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(null);

        JLabel usernameLabel = new JLabel("아이디:");
        usernameLabel.setBounds(20, 20, 80, 25);
        JTextField usernameField = new JTextField();
        usernameField.setBounds(100, 20, 150, 25);

        JLabel passwordLabel = new JLabel("비밀번호:");
        passwordLabel.setBounds(20, 60, 80, 25);
        JPasswordField passwordField = new JPasswordField();
        passwordField.setBounds(100, 60, 150, 25);

        JButton loginButton = new JButton("로그인");
        loginButton.setBounds(40, 110, 100, 30);

        JButton registerButton = new JButton("회원가입");
        registerButton.setBounds(160, 110, 100, 30);

        // MyListener를 사용하여 로그인 처리
        loginButton.addActionListener(new MyListener(frame, usernameField, passwordField));

        // 회원가입 버튼
        registerButton.addActionListener(e -> {
            frame.dispose();
            new RegisterUI();
        });

        frame.add(usernameLabel);
        frame.add(usernameField);
        frame.add(passwordLabel);
        frame.add(passwordField);
        frame.add(loginButton);
        frame.add(registerButton);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        new LoginUI();
    }

    class MyListener implements ActionListener {
        private JFrame frame;
        private JTextField usernameField;
        private JPasswordField passwordField;

        public MyListener(JFrame frame, JTextField usernameField, JPasswordField passwordField) {
            this.frame = frame;
            this.usernameField = usernameField;
            this.passwordField = passwordField;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword()).trim();

            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "아이디와 비밀번호를 입력하세요.", "오류", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String company = DBConnector.loginUser(username, password);
            int companyId = DBConnector.getCompanyIdByName(company);
            if (company != null) {
                int userId = DBConnector.getUserIdByUsername(username); // 사용자 ID 가져오기
                String name = DBConnector.getUserName(username); // 사용자 이름 검색
                JOptionPane.showMessageDialog(frame, "로그인 성공!", "성공", JOptionPane.INFORMATION_MESSAGE);
                frame.dispose();
                new CalendarUI(name, company, userId, companyId); // 사용자 이름, 회사 이름, 사용자 ID 전달
            } else {
                JOptionPane.showMessageDialog(frame, "로그인 실패. 아이디와 비밀번호를 확인하세요.", "오류", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
