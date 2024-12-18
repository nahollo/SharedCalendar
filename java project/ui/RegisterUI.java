package ui;

import javax.swing.*;
import java.awt.event.*;
import server.DBConnector;

public class RegisterUI {
    public RegisterUI() {
        JFrame frame = new JFrame("회원가입");
        frame.setSize(350, 350);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(null);

        JLabel usernameLabel = new JLabel("아이디:");
        usernameLabel.setBounds(15, 20, 80, 25);
        JTextField usernameField = new JTextField();
        usernameField.setBounds(100, 20, 200, 25);

        JLabel nameLabel = new JLabel("이름:");
        nameLabel.setBounds(15, 60, 80, 25);
        JTextField nameField = new JTextField();
        nameField.setBounds(100, 60, 200, 25);

        JLabel passwordLabel = new JLabel("비밀번호:");
        passwordLabel.setBounds(15, 100, 80, 25);
        JPasswordField passwordField = new JPasswordField();
        passwordField.setBounds(100, 100, 200, 25);

        JLabel confirmPasswordLabel = new JLabel("비밀번호 확인:");
        confirmPasswordLabel.setBounds(15, 140, 100, 25);
        JPasswordField confirmPasswordField = new JPasswordField();
        confirmPasswordField.setBounds(100, 140, 200, 25);

        JLabel companyLabel = new JLabel("회사명:");
        companyLabel.setBounds(15, 180, 80, 25);
        JTextField companyField = new JTextField();
        companyField.setBounds(100, 180, 200, 25);

        JButton registerButton = new JButton("회원가입");
        registerButton.setBounds(65, 240, 100, 30);

        JButton backButton = new JButton("뒤로가기");
        backButton.setBounds(185, 240, 100, 30);

        registerButton.addActionListener(new RegisterListener(frame, usernameField, nameField, passwordField,
                confirmPasswordField, companyField));

        backButton.addActionListener(e -> {
            frame.dispose();
            new LoginUI();
        });

        frame.add(nameLabel);
        frame.add(nameField);
        frame.add(usernameLabel);
        frame.add(usernameField);
        frame.add(passwordLabel);
        frame.add(passwordField);
        frame.add(confirmPasswordLabel);
        frame.add(confirmPasswordField);
        frame.add(companyLabel);
        frame.add(companyField);
        frame.add(registerButton);
        frame.add(backButton);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}

class RegisterListener implements ActionListener {
    private JFrame frame;
    private JTextField usernameField;
    private JTextField nameField;
    private JPasswordField passwordField;
    private JPasswordField confirmPasswordField;
    private JTextField companyField;

    public RegisterListener(JFrame frame, JTextField usernameField, JTextField nameField,
            JPasswordField passwordField, JPasswordField confirmPasswordField, JTextField companyField) {
        this.frame = frame;
        this.usernameField = usernameField;
        this.nameField = nameField;
        this.passwordField = passwordField;
        this.confirmPasswordField = confirmPasswordField;
        this.companyField = companyField;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String username = usernameField.getText().trim();
        String name = nameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();
        String confirmPassword = new String(confirmPasswordField.getPassword()).trim();
        String company = companyField.getText().trim();

        if (username.isEmpty() || name.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()
                || company.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "모든 필드를 입력하세요.", "오류", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!password.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(frame, "비밀번호가 일치하지 않습니다.", "오류", JOptionPane.WARNING_MESSAGE);
            return;
        }

        boolean registerSuccess = DBConnector.registerUser(username, password, company, name);

        if (registerSuccess) {
            JOptionPane.showMessageDialog(frame, "회원가입 성공!", "성공", JOptionPane.INFORMATION_MESSAGE);
            frame.dispose();
            new LoginUI();
        } else {
            JOptionPane.showMessageDialog(frame, "회원가입 실패: 중복된 아이디가 있습니다.", "오류", JOptionPane.ERROR_MESSAGE);
        }
    }
}
