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

    // 로그인 메서드 (사용자 검증 및 회사 정보 반환)
    public static String loginUser(String username, String password) {
        String query = "SELECT company_id FROM users WHERE username = ? AND password = ?";
        try (Connection conn = connect();
                PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return getCompanyName(rs.getInt("company_id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // 사용자 이름 검색 메서드
    public static String getUserName(String username) {
        String query = "SELECT name FROM users WHERE username = ?";
        try (Connection conn = connect();
                PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("name");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // 회사 이름 가져오기
    private static String getCompanyName(int companyId) {
        String query = "SELECT name FROM companies WHERE id = ?";
        try (Connection conn = connect();
                PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, companyId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("name");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<String> getUsersInCompany(String companyName, String excludeUsername) {
        List<String> users = new ArrayList<>();
        String query = """
                    SELECT u.name
                    FROM users u
                    JOIN companies c ON u.company_id = c.id
                    WHERE c.name = ? AND u.name != ?
                """;
        try (Connection conn = connect();
                PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, companyName); // 회사 이름 조건
            stmt.setString(2, excludeUsername); // 제외할 사용자 조건
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                users.add(rs.getString("name")); // 사용자 이름 추가
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    // 사용자 등록 메서드 (아이디 중복 체크 포함)
    public static boolean registerUser(String username, String password, String companyName, String name) {
        Connection conn = null;
        try {
            conn = connect();
            conn.setAutoCommit(false); // 트랜잭션 시작

            // 아이디 중복 체크
            String checkUserQuery = "SELECT COUNT(*) FROM users WHERE username = ?";
            try (PreparedStatement checkUserStmt = conn.prepareStatement(checkUserQuery)) {
                checkUserStmt.setString(1, username);
                ResultSet userRs = checkUserStmt.executeQuery();
                if (userRs.next() && userRs.getInt(1) > 0) {
                    conn.rollback();
                    return false; // 중복된 아이디가 존재
                }
            }

            // 회사 정보 확인 및 추가
            int companyId;
            String checkCompanyQuery = "SELECT id FROM companies WHERE name = ?";
            String insertCompanyQuery = "INSERT INTO companies (name) VALUES (?)";
            try (PreparedStatement checkCompanyStmt = conn.prepareStatement(checkCompanyQuery)) {
                checkCompanyStmt.setString(1, companyName);
                ResultSet companyRs = checkCompanyStmt.executeQuery();
                if (companyRs.next()) {
                    companyId = companyRs.getInt("id");
                } else {
                    try (PreparedStatement insertCompanyStmt = conn.prepareStatement(insertCompanyQuery,
                            Statement.RETURN_GENERATED_KEYS)) {
                        insertCompanyStmt.setString(1, companyName);
                        insertCompanyStmt.executeUpdate();
                        ResultSet generatedKeys = insertCompanyStmt.getGeneratedKeys();
                        if (generatedKeys.next()) {
                            companyId = generatedKeys.getInt(1);
                        } else {
                            conn.rollback();
                            return false;
                        }
                    }
                }
            }

            // 사용자 정보 추가
            String insertUserQuery = "INSERT INTO users (username, password, company_id, name) VALUES (?, ?, ?, ?)";
            try (PreparedStatement insertUserStmt = conn.prepareStatement(insertUserQuery)) {
                insertUserStmt.setString(1, username);
                insertUserStmt.setString(2, password);
                insertUserStmt.setInt(3, companyId);
                insertUserStmt.setString(4, name);
                insertUserStmt.executeUpdate();
            }

            conn.commit(); // 트랜잭션 커밋
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            if (conn != null) {
                try {
                    conn.rollback(); // 오류 발생 시 롤백
                } catch (SQLException rollbackEx) {
                    rollbackEx.printStackTrace();
                }
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.close(); // 연결 닫기
                } catch (SQLException closeEx) {
                    closeEx.printStackTrace();
                }
            }
        }
    }

    // 메시지 저장 메서드 추가
    public static void saveMessage(int chatRoomId, int senderId, String content) {
        String query = "INSERT INTO messages (chat_room_id, sender_id, content, sent_at) VALUES (?, ?, ?, NOW())";
        try (Connection conn = connect();
                PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, chatRoomId);
            stmt.setInt(2, senderId);
            stmt.setString(3, content);
            stmt.executeUpdate();
            System.out.println("메시지 저장 완료 (개인채팅): 채팅방ID=" + chatRoomId + ", 보낸사람ID=" + senderId + ", 내용=" + content);
        } catch (SQLException e) {
            System.err.println("개인 채팅 메시지 저장 중 오류: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 채팅방 ID 가져오기
    public static int getChatRoomId(int companyId, String roomName, boolean isGroup) {
        String query = """
                    SELECT id FROM chat_rooms
                    WHERE company_id = ? AND name = ? AND is_group = ?
                """;
        try (Connection conn = connect();
                PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, companyId); // 회사 ID
            stmt.setString(2, roomName); // 방 이름 (개인 혹은 단체 채팅)
            stmt.setBoolean(3, isGroup); // 단체 여부
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id"); // 채팅방 ID 반환
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1; // 방을 찾을 수 없는 경우
    }

    // 사용자 이름 가져오기
    public static String getUserNameById(int userId) {
        String query = "SELECT name FROM users WHERE id = ?";
        try (Connection conn = connect();
                PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("name");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static int getUserIdByUsername(String username) {
        String query = "SELECT id FROM users WHERE username = ?";
        try (Connection conn = connect();
                PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username); // username 값 설정
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id"); // 사용자 ID 반환
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        System.err.println("데이터베이스에서 username을 찾을 수 없음: " + username);
        return -1; // 사용자 ID를 찾을 수 없는 경우
    }

    public static int getUserIdByname(String username) {
        String query = "SELECT id FROM users WHERE name = ?";
        try (Connection conn = connect();
                PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username); // username 값 설정
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id"); // 사용자 ID 반환
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        System.err.println("데이터베이스에서 username을 찾을 수 없음: " + username);
        return -1; // 사용자 ID를 찾을 수 없는 경우
    }

    public static int getUserIdByname(String name, int companyId) {
        String query = "SELECT id FROM users WHERE name = ? AND company_id = ?";
        try (Connection conn = connect();
                PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, name);
            stmt.setInt(2, companyId); // 회사 ID 추가 조건
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static int getUserIdByName(String name) {
        String query = "SELECT id FROM users WHERE name = ?";
        try (Connection conn = connect();
                PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        System.err.println("데이터베이스에서 name을 찾을 수 없음: " + name);
        return -1;
    }

    public static List<String> getUsersByCompany(int companyId) {
        List<String> users = new ArrayList<>();
        String query = "SELECT name FROM users WHERE company_id = ?";
        try (Connection conn = connect();
                PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, companyId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                users.add(rs.getString("name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    public static List<String> getChatHistory(int chatRoomId) {
        List<String> messages = new ArrayList<>();
        String query = "SELECT sender_id, content, sent_at FROM messages WHERE chat_room_id = ? ORDER BY sent_at ASC";
        try (Connection conn = connect();
                PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, chatRoomId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String sender = getUserNameById(rs.getInt("sender_id"));
                String content = rs.getString("content");
                String timestamp = rs.getString("sent_at");
                messages.add(sender + "||" + content + "||" + timestamp);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return messages;
    }

    public static int getChatRoomId(int userId1, int userId2) {
        String query = """
                    SELECT id FROM chat_rooms
                    WHERE user1_id = LEAST(?, ?)
                    AND user2_id = GREATEST(?, ?)
                """;
        try (Connection conn = connect();
                PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, userId1);
            stmt.setInt(2, userId2);
            stmt.setInt(3, userId1);
            stmt.setInt(4, userId2);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1; // 채팅방을 찾을 수 없는 경우
    }

    // 개인 채팅방 생성
    public static int createChatRoom(int userId1, int userId2, boolean isGroup, int companyId) {
        String query = """
                    INSERT INTO chat_rooms (user1_id, user2_id, is_group, company_id)
                    VALUES (LEAST(?, ?), GREATEST(?, ?), ?, ?)
                """;
        try (Connection conn = connect();
                PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, userId1);
            stmt.setInt(2, userId2);
            stmt.setInt(3, userId1);
            stmt.setInt(4, userId2);
            stmt.setBoolean(5, isGroup);
            stmt.setInt(6, companyId);
            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1); // 새로 생성된 채팅방 ID 반환
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static int getCompanyIdByName(String companyName) {
        String query = "SELECT id FROM companies WHERE name = ?";
        try (Connection conn = connect();
                PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, companyName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1; // 회사 ID를 찾을 수 없는 경우
    }

    // 단체 채팅방 ID 가져오기
    public static int getChatRoomIdForGroupChat(int companyId) {
        String selectQuery = "SELECT id FROM chat_rooms WHERE company_id = ? AND is_group = TRUE";
        String insertQuery = "INSERT INTO chat_rooms (company_id, name, is_group) VALUES (?, '단체 채팅방', TRUE)";
        try (Connection conn = connect()) {
            // 단체 채팅방 검색
            try (PreparedStatement selectStmt = conn.prepareStatement(selectQuery)) {
                selectStmt.setInt(1, companyId);
                ResultSet rs = selectStmt.executeQuery();
                if (rs.next()) {
                    int chatRoomId = rs.getInt("id");
                    System.out.println("단체 채팅방 ID: " + chatRoomId);
                    return chatRoomId;
                }
            }

            // 단체 채팅방 생성
            try (PreparedStatement insertStmt = conn.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS)) {
                insertStmt.setInt(1, companyId);
                insertStmt.executeUpdate();
                ResultSet generatedKeys = insertStmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    int chatRoomId = generatedKeys.getInt(1);
                    System.out.println("단체 채팅방 생성 완료, ID: " + chatRoomId);
                    return chatRoomId;
                }
            }
        } catch (SQLException e) {
            System.err.println("단체 채팅방 생성 또는 검색 중 오류: " + e.getMessage());
            e.printStackTrace();
        }
        return -1;
    }

    // 단체 채팅방 생성
    public static int createGroupChatRoom(int companyId) {
        // 기존 단체 채팅방이 있으면 ID 반환
        int existingRoomId = getChatRoomIdForGroupChat(companyId);
        if (existingRoomId != -1) {
            return existingRoomId;
        }

        // 새 단체 채팅방 생성
        String query = """
                INSERT INTO chat_rooms (company_id, name, is_group)
                VALUES (?, '단체 채팅방', TRUE)
                """;
        try (Connection conn = connect();
                PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, companyId);
            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1); // 새로 생성된 채팅방 ID 반환
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    // 단체 채팅 메시지 저장
    public static void saveGroupChatMessage(int chatRoomId, int senderId, String content) {
        String query = "INSERT INTO messages (chat_room_id, sender_id, content) VALUES (?, ?, ?)";
        try (Connection conn = connect();
                PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, chatRoomId);
            stmt.setInt(2, senderId);
            stmt.setString(3, content);
            stmt.executeUpdate();
            System.out.println("단체 채팅 메시지 저장 완료: 채팅방ID=" + chatRoomId + ", 보낸사람ID=" + senderId);
        } catch (SQLException e) {
            System.err.println("단체 채팅 메시지 저장 실패: " + e.getMessage());
        }
    }

    // 단체 채팅 메시지 조회
    public static List<String> getGroupChatHistory(int chatRoomId) {
        List<String> messages = new ArrayList<>();
        String query = """
                SELECT m.sender_id, u.name AS sender_name, m.content, m.sent_at
                FROM messages m
                JOIN users u ON m.sender_id = u.id
                WHERE m.chat_room_id = ?
                ORDER BY m.sent_at
                """;
        try (Connection conn = connect();
                PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, chatRoomId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String senderName = rs.getString("sender_name");
                String content = rs.getString("content");
                String timestamp = rs.getString("sent_at");
                messages.add(senderName + "||" + content + "||" + timestamp);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return messages;
    }

}
