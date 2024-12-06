package ui;

import javax.swing.*;

public class RegisterUI {
    public RegisterUI() {
        JFrame frame = new JFrame("회원가입");
        frame.setSize(350, 250);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(null);

        JLabel usernameLabel = new JLabel("아이디:");
        usernameLabel.setBounds(20, 20, 80, 25);
        JTextField usernameField = new JTextField();
        usernameField.setBounds(100, 20, 200, 25);

        JLabel passwordLabel = new JLabel("비밀번호:");
        passwordLabel.setBounds(20, 60, 80, 25);
        JPasswordField passwordField = new JPasswordField();
        passwordField.setBounds(100, 60, 200, 25);

        JLabel companyLabel = new JLabel("회사명:");
        companyLabel.setBounds(20, 100, 80, 25);
        JTextField companyField = new JTextField();
        companyField.setBounds(100, 100, 200, 25);

        JButton registerButton = new JButton("회원가입");
        registerButton.setBounds(100, 150, 100, 30);

        // ActionListener 등록
        registerButton.addActionListener(new MyListener(frame, usernameField, passwordField, companyField, false));

        frame.add(usernameLabel);
        frame.add(usernameField);
        frame.add(passwordLabel);
        frame.add(passwordField);
        frame.add(companyLabel);
        frame.add(companyField);
        frame.add(registerButton);
        frame.setVisible(true);
    }
}
