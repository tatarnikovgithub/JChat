package ru.jchat.core.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.Vector;

public class Server {
    private Vector<ClientHandler> clients;
    private AuthService authService;

    public AuthService getAuthService() {
        return authService;
    }

    public Server(){
        try(ServerSocket serverSocket = new ServerSocket(8189)){
            clients = new Vector<>();
            authService = new AuthService();
            authService.connect();
            System.out.println("Server started... Waiting clients");
            while (true){
                Socket socket = serverSocket.accept();
                System.out.println("Client connected " + socket.getInetAddress() + " " + socket.getPort() + " " + socket.getLocalPort());
                new ClientHandler(this, socket);
            }
        } catch (IOException e){
            e.printStackTrace();
        } catch (SQLException | ClassNotFoundException e){
            System.out.println("Не удалось запустить сервис авторизации");
            e.printStackTrace();
        } finally {
            authService.disconnect();
        }
    }

    public void subscribe(ClientHandler clientHandler){
        clients.add(clientHandler);
        broadcastClientsList();

    }

    public void unsubscribe(ClientHandler clientHandler){
        clients.remove(clientHandler);
        broadcastClientsList();
    }

    public boolean isNickBusy(String nick){
        for (ClientHandler o: clients){
            if (o.getNick().equals(nick)){
                return true;
            }
        }
        return false;
    }

    public void broadcastMsg(String msg){
        for (ClientHandler o: clients){
            o.sendMsg(msg);
        }
    }

    public void sendPrivateMsg(ClientHandler from, String nickTo, String msg){
        for(ClientHandler o: clients){
            if (o.getNick().equals(nickTo)){
                o.sendMsg("от " + from.getNick() + ": " + msg);
                from.sendMsg("клиенту " + nickTo + ": " + msg);
                return;
            }
        }
        from.sendMsg("Клиент с ником " + nickTo + " не найден");
    }

    public void broadcastClientsList(){
        StringBuilder sb = new StringBuilder("/clientslist ");
        for (ClientHandler o: clients){
            sb.append(o.getNick() + " ");
        }
        String out = sb.toString();
        for (ClientHandler o: clients){
            o.sendMsg(out);
        }
    }
}
