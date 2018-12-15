package ru.jchat.core.server;

import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.*;

public class ClientHandler {
    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String nick;
    private boolean isLoggedIn;

    public String getNick() {
        return nick;
    }

    public ClientHandler(Server server, Socket socket) {
        try {
            this.server = server;
            this.socket = socket;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            new Thread(() -> {
                try{
                    while(true){
                        String msg = in.readUTF();
                        String[] data = msg.split("\\s");
                        if (msg.startsWith("/auth ") && data.length > 2){
                            String newNick = server.getAuthService().getNickByLoginAndPass(data[1], data[2]);
                            if (newNick != null){
                                if (!server.isNickBusy(newNick)){
                                    nick = newNick;
                                    sendMsg("/authok");
                                    server.subscribe(this);
                                    isLoggedIn = true;
                                    break;
                                } else {
                                    sendMsg("Учетная запись уже занята");
                                }
                            } else {
                                sendMsg("Неверный логин/пароль");
                            }
                        }
                    }
                    while(true){
                        String msg = in.readUTF();
                        System.out.println(nick + ": " + msg);
                        if (msg.startsWith("/")){
                            if (msg.equals("/end")) break;
                        } else {
                            server.broadcastMsg(nick + ": " + msg);
                        }
                    }
                }catch (IOException e){
                    e.printStackTrace();
                }finally {
                    nick = null;
                    server.unsubscribe(this);
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }).start();

            new Thread(() -> {
                try {
                    Thread.sleep(120000);
                    if (!isLoggedIn) {
                        socket.close();
                        System.out.println("Socket closed.");
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMsg(String msg){
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
