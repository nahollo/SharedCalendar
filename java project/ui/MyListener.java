package ui;

import server.DBConnector;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MyListener implements ActionListener {
    private JFrame frame;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JTextField companyField;
    private boolean isLogin;

    public MyListener(JFrame frame, JTextField usernameField, JPasswordField passwordField, JTextField companyField, boolean isLogin) {
        this.frame = frame;
        this.usernameField = usernameField;
        this.passwordField = passwordField;
        this.companyField = companyField;
        this.isLogin = isLogin;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());

        if (isLogin) {
            boolean success = DBConnector.loginUser(username, password);
            if (success) {
                JOptionPane.showMessageDialog(frame, "로그인 성공!");
                frame.dispose(); // 로그인 성공 시 창 닫기
                // 로그인 후 처리 (예: 캘린더 화면 열기)
            } else {
                JOptionPane.showMessageDialog(frame, "로그인 실패! 아이디와 비밀번호를 확인하세요.");
            }
        } else {
            String companyName = companyField.getText();
            boolean success = DBConnector.registerUser(username, password, companyName);
            if (success) {
                JOptionPane.showMessageDialog(frame, "회원가입 성공!");
                frame.dispose(); // 회원가입 성공 시 창 닫기
                // 회원가입 후 처리 (예: 로그인 화면으로 이동)
            } else {
                JOptionPane.showMessageDialog(frame, "회원가입 실패! 다시 시도하세요.");
            }
        }
    }
}
