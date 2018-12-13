package ru.jchat.core.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Vector;

class Server {
    private Vector<ClientHandler> clients;
    private AuthService authService;

    AuthService getAuthService() {
        return authService;
    }

    Server(){
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
        } finally {
            Objects.requireNonNull(authService).disconnect();
        }
    }

    void subscribe(ClientHandler clientHandler){
        clients.add(clientHandler);
    }

    void unsubscribe(ClientHandler clientHandler){
        clients.remove(clientHandler);
    }

    boolean isNickBusy(String nick){
        boolean isNickBusy = false;
        for (ClientHandler o: clients){
            if (o.getNick().equals(nick)){
              isNickBusy = true;
              break;
            }
        }
        return isNickBusy;
    }

    void broadcastMsg(String msg){
        for (ClientHandler o: clients){
            o.sendMsg(msg);
        }
    }

    void privateMsg (String toNick, String msg, String sender) throws UnknownClientException {
      boolean sended = false;

      for (ClientHandler o: clients){
        if (o.getNick().equals(toNick)){
          o.sendMsg(String.format("PRIVMSG FROM %s:%n%s", sender, msg));
          sended = true;
          break;
        }
      }

      if (!sended){
        throw new UnknownClientException();
      }
    }
}
