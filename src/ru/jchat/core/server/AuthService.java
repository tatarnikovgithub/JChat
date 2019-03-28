package ru.jchat.core.server;

import java.sql.*;

public class AuthService {
    private Connection connection;
    private Statement stmt;
    private PreparedStatement psFindNick;
    private PreparedStatement psUserRegister;
    private PreparedStatement psChangeNick;
    private PreparedStatement psGetId;


    public void connect() throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:main.db");
        stmt = connection.createStatement();
        checkTable();
        psFindNick = connection.prepareStatement("SELECT nick FROM users WHERE login = ? AND password = ?;");
        psUserRegister = connection.prepareStatement("INSERT INTO users (login, password, nick) VALUES (?,?,?);");
        psChangeNick = connection.prepareStatement("UPDATE users SET nick = ? WHERE id = ?;");
        psGetId = connection.prepareStatement("SELECT id FROM users WHERE nick = ?;");
        //        userRegistration("login1", "pass1", "nick1");
        testUsers();
    }

    public boolean changeNick(int id, String newNick) throws SQLException {
        try{
            psChangeNick.setString(1, newNick);
            psChangeNick.setInt(2, id);
            return psChangeNick.executeUpdate() == 1;
        } catch (SQLException e){
            //            if (e.getCause().getMessage().contains("UNIQUE constraint failed: users.nick")){
            return false;
        }
    }

    public int getAuthorizedIdByNick(String nick) throws SQLException {
        psGetId.setString(1, nick);
        ResultSet rs = psGetId.executeQuery();
        return rs.getInt("id");
    }

    public void testUsers() throws SQLException {
        stmt.execute("DELETE FROM users;");
        for (int i = 1; i <= 5; i++){
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
        try{
            int passHash = pass.hashCode();
            psUserRegister.setString(1, login);
            psUserRegister.setInt(2, passHash);
            psUserRegister.setString(3, nick);
            return psUserRegister.executeUpdate() == 1;
        } catch (SQLException e){
            throw new SQLException("Ошибка регистрации пользователя");
        }
    }

    public String getNickByLoginAndPass(String login, String pass) {
        try{
            psFindNick.setString(1, login);
            int passHash = pass.hashCode();
            psFindNick.setInt(2, passHash);
            ResultSet rs = psFindNick.executeQuery();
            if (rs.next()){
                return rs.getString("nick");
            }
        } catch (SQLException e){
            e.printStackTrace();
        }
        return null;
    }

    public void disconnect() {
        try{
            stmt.close();
            connection.close();
        } catch (SQLException e){
            e.printStackTrace();
        }
    }
}
