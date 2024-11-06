
import javax.swing.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class LoginUI extends JFrame {
    private JTextField emailField;
    private JPasswordField passwordField;
    private CalendarClient client;

    public LoginUI(CalendarClient client) {
        this.client = client;

        setTitle("로그인");
        setSize(300, 150);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridLayout(3, 2, 5, 5));

        add(new JLabel("이메일:"));
        emailField = new JTextField();
        add(emailField);

        add(new JLabel("비밀번호:"));
        passwordField = new JPasswordField();
        add(passwordField);

        JButton loginButton = new JButton("로그인");
        loginButton.addActionListener(new LoginAction());
        add(loginButton);

        JButton registerButton = new JButton("회원가입");
        registerButton.addActionListener(e -> new RegisterUI(client));
        add(registerButton);

        setVisible(true);
    }

    private class LoginAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String email = emailField.getText().trim();
            String password = new String(passwordField.getPassword());

            if (client.login(email, password)) {
                JOptionPane.showMessageDialog(LoginUI.this, "로그인 성공!");
                dispose();
                new CalendarUI(client);
            } else {
                JOptionPane.showMessageDialog(LoginUI.this, "로그인 실패. 이메일과 비밀번호를 확인하세요.", "오류",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
