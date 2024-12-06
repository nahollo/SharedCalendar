package ui;

import javax.swing.*;

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
        loginButton.setBounds(50, 100, 100, 30);

        JButton registerButton = new JButton("회원가입");
        registerButton.setBounds(160, 100, 100, 30);

        // ActionListener 등록
        loginButton.addActionListener(new MyListener(frame, usernameField, passwordField, null, true));
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
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        new LoginUI();
    }
}
