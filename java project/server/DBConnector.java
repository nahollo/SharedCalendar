package server;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DBConnector {
    private static final String URL = "jdbc:mysql://localhost:3307/SharedCalendar";
    private static final String USER = "root";
    private static final String PASSWORD = "1234";

    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    // 회원가입 기능
    public static boolean registerUser(String username, String password, String companyName) {
        String checkCompanyQuery = "SELECT id FROM companies WHERE name = ?";
        String insertUserQuery = "INSERT INTO users (username, password, company_id) VALUES (?, ?, ?)";
        String insertCompanyQuery = "INSERT INTO companies (name) VALUES (?)";

        try (Connection conn = connect()) {
            // 회사 정보 확인 및 추가
            int companyId;
            PreparedStatement checkCompanyStmt = conn.prepareStatement(checkCompanyQuery);
            checkCompanyStmt.setString(1, companyName);
            ResultSet rs = checkCompanyStmt.executeQuery();
            if (rs.next()) {
                companyId = rs.getInt("id");
            } else {
                PreparedStatement insertCompanyStmt = conn.prepareStatement(insertCompanyQuery,
                        Statement.RETURN_GENERATED_KEYS);
                insertCompanyStmt.setString(1, companyName);
                insertCompanyStmt.executeUpdate();
                rs = insertCompanyStmt.getGeneratedKeys();
                if (rs.next()) {
                    companyId = rs.getInt(1);
                } else {
                    return false;
                }
            }

            // 사용자 정보 추가
            PreparedStatement insertUserStmt = conn.prepareStatement(insertUserQuery);
            insertUserStmt.setString(1, username);
            insertUserStmt.setString(2, password);
            insertUserStmt.setInt(3, companyId);
            insertUserStmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 로그인 기능
    public static boolean loginUser(String username, String password) {
        String query = "SELECT * FROM users WHERE username = ? AND password = ?";
        try (Connection conn = connect();
                PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 캘린더 기능: 일정 추가
    public static boolean addSchedule(String startDate, String endDate, String title, String description,
            int companyId) {
        String query = "INSERT INTO events (company_id, title, description, start_time, end_time) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = connect();
                PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, companyId);
            stmt.setString(2, title);
            stmt.setString(3, description);
            stmt.setString(4, startDate);
            stmt.setString(5, endDate);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 캘린더 기능: 일정 조회
    public static List<String> getSchedulesByDate(String date, int companyId) {
        String query = "SELECT title, start_time, end_time FROM events WHERE company_id = ? AND DATE(start_time) <= ? AND DATE(end_time) >= ?";
        List<String> schedules = new ArrayList<>();
        try (Connection conn = connect();
                PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, companyId);
            stmt.setString(2, date);
            stmt.setString(3, date);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String title = rs.getString("title");
                String startTime = rs.getString("start_time");
                String endTime = rs.getString("end_time");
                schedules.add(title + " (" + startTime + " ~ " + endTime + ")");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return schedules;
    }

    // 캘린더 기능: 일정 삭제
    public static boolean deleteSchedule(int eventId) {
        String query = "DELETE FROM events WHERE id = ?";
        try (Connection conn = connect();
                PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, eventId);
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
