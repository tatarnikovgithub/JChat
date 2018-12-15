package ru.jchat.core.server;

import java.sql.*;

public class AuthService {
    private Connection connection;
    private Statement stmt;
    private PreparedStatement psFindNick;
    private PreparedStatement psUserRegister;


    public void connect() throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:main.db");
        stmt = connection.createStatement();
        checkTable();
        psFindNick = connection.prepareStatement("SELECT nick FROM users WHERE login = ? AND password = ?;");
        psUserRegister = connection.prepareStatement("INSERT INTO users (login, password, nick) VALUES (?,?,?);");
//        userRegistration("login1", "pass1", "nick1");
        testUsers();
    }

    public void testUsers() throws SQLException {
        stmt.execute("DELETE FROM users;");
        for (int i = 1; i <= 20 ; i++) {
            userRegistration("login" + i, "pass" + i, "nick" + i);
        }
    }

    public void checkTable() throws SQLException {
        stmt.execute("CREATE TABLE IF NOT EXISTS users (\n" +
                "    id       INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                "    login    TEXT    UNIQUE,\n" +
                "    password INTEGER,\n" +
                "    nick     TEXT    UNIQUE\n" +
                ");");
    }

    public boolean userRegistration(String login, String pass, String nick) throws SQLException {
        try {
            int passHash = pass.hashCode();
            psUserRegister.setString(1, login);
            psUserRegister.setInt(2, passHash);
            psUserRegister.setString(3, nick);
            return psUserRegister.executeUpdate()== 1;
        } catch (SQLException e) {
            throw new SQLException("Ошибка регистрации пользователя");
        }
    }

    public String getNickByLoginAndPass(String login, String pass){
        try {
            psFindNick.setString(1, login);
            int passHash = pass.hashCode();
            psFindNick.setInt(2, passHash);
            ResultSet rs = psFindNick.executeQuery();
            if(rs.next()){
                return rs.getString("nick");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void disconnect(){
        try {
            stmt.close();
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
