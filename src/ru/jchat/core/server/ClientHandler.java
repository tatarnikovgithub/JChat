package ru.jchat.core.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

class ClientHandler {
  private Server server;
  private Socket socket;
  private DataInputStream in;
  private DataOutputStream out;
  private String nick;


  ClientHandler(Server server, Socket socket) {
    try {
      this.server = server;
      this.socket = socket;
      this.in = new DataInputStream(socket.getInputStream());
      this.out = new DataOutputStream(socket.getOutputStream());

      new Thread(() -> {
        try{
          exit:
          while( socket.isConnected() ){//socket.isConnected()
            String msg = in.readUTF();
            System.out.println(nick + ": " + msg);

            if ( !msg.trim().isEmpty() ){
              if (msg.startsWith("/")){
                String cmd = msg.split("\\s", 2)[0];
                switch (cmd){
                  case "/w":
                    sendPrivateMsg(msg);
                    break;
                  case "/auth":
                    auth(msg);
                    break;
                  case "/end":
                    break exit;
                }
              }
              else {
                server.broadcastMsg(nick + ": " + msg);
              }
            }
          }
        }
        catch (SocketException e){
          System.out.printf("%s: client has disconnected%n", nick);
        }
        catch (IOException e){
          e.printStackTrace();
        }
        finally {
          nick = null;
          server.unsubscribe(this);
          try {
              socket.close();
          } catch (IOException e) {
              e.printStackTrace();
          }
        }
      }).start();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void auth (String msg){
    String[] data = msg.split("\\s");
    String newNick = server.getAuthService().getNickByLoginAndPass(data[1], data[2]);
    if (newNick != null){
      if (!server.isNickBusy(newNick)){
        nick = newNick;
        sendMsg("/authok");
        server.subscribe(this);
      }
      else {
        sendMsg("Учетная запись уже занята");
      }
    }
    else {
      sendMsg("Неверный логин/пароль");
    }
  }

  String getNick() {
    return nick;
  }

  void sendMsg(String msg){
    try {
      out.writeUTF(msg);
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void sendPrivateMsg(String msg){
    // /w nick3 Привет привет!
    String[] data = msg.split("\\s", 3);
    try{
      if (!data[1].trim().isEmpty() && !data[2].trim().isEmpty()){
        server.privateMsg(data[1], data[2], nick);
        sendMsg(String.format("PRIVMSG TO %s:%n%s", data[1], data[2]));
      }
    }
    catch (ArrayIndexOutOfBoundsException| NullPointerException e){
      sendMsg(String.format(
        "Ошибка отправки. Введите приватное сообщение в формате:%n%s",
        "/w nick_получателя текст_сообщения"
      ));
    }
    catch (UnknownClientException e){
      sendMsg("Не удалось доставить сообщение, проверьте ник получателя.");
    }
  }
}
