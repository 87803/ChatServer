package com.example.chatserver;

import java.net.Socket;

public class UserInfo {
    String nickName;
    Socket clientSocket;
    UserInfo(String nickName, Socket clientSocket)
    {
        this.nickName=nickName;
        this.clientSocket=clientSocket;
    }
}
