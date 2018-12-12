package ru.jchat.core.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {
    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String nick;

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
                        if (msg.startsWith("/auth ")){
                            String[] data = msg.split("\\s");
                            String newNick = server.getAuthService().getNickByLoginAndPass(data[1], data[2]);
                            if (newNick != null){
                                if (!server.isNickBusy(newNick)){
                                    nick = newNick;
                                    sendMsg("/authok");
                                    server.subscribe(this);
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
                            if (msg.startsWith("/w")) {
                                try {
                                    String[] parsedMsg = parseMsg(msg);
                                    server.sendPrivateMsg(parsedMsg[0], parsedMsg[1]);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
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

    private String[] parseMsg(String msg) throws Exception {
        int nickStart;
        for (nickStart = 2; nickStart < msg.length(); nickStart++) {
            if (msg.charAt(nickStart) != ' ') {
                break;
            }
        }
        if (nickStart == msg.length()) {
            throw new Exception("Message doesn't contain nickname.");
        }
        int nickFinish = msg.indexOf(' ', nickStart);
        if (nickFinish == -1) {
            throw new Exception("Message doesn't contain text.");
        }
        String[] result = new String[2];
        result[0] = msg.substring(nickStart, nickFinish);
        result[1] = msg.substring(nickFinish + 1);
        return result;
    }
}
