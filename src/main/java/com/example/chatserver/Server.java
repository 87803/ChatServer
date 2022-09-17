package com.example.chatserver;

import com.alibaba.fastjson.JSON;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Server implements Runnable{
    private ServerSocket serverSocket;
    static int userNum;
    static Connection dbConn;
    static Map<String, UserInfo> onlineUser;
    public static MainController mainWindow;

    Server(int port, MainController mainWindow) {
        Server.mainWindow = mainWindow;
        String ipAddress = "127.0.0.1";
        userNum = 0;
        Server.mainWindow.updateWindow(ipAddress, port);
        Server.mainWindow.updateUserNum(0);
        Server.onlineUser = new HashMap<>();
        initSqlServer();
        try
        {
            serverSocket = new ServerSocket(port);
            new Thread(this).start();
        }
        catch  (IOException e)
        {
            System.out.println("服务器启动失败！");
        }
    }

    private void initSqlServer()
    {
        String dbURL = "jdbc:sqlserver://localhost:1433;DatabaseName=chatServer;trustServerCertificate=true;trustStorePassword=false";//数据库路径，不使用SSL加密和不在jvm中存储密码
        String name = "sa";                                                            //数据库账号
        String password = "123";//数据库密码
        //String select = "select * from [StockHistory]";//简单查询语句
        try {
            //1.加载驱动
            //Class.forName方法的作用,就是初始化给定的类.而我们给定的MySQL的Driver类中,
            // 它在静态代码块中通过JDBC的DriverManager注册了一下驱动.我们也可以直接使用JDBC的驱动管理器注册mysql驱动.
            // 从而代替使用Class.forName.
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");

            //2.连接
            Server.dbConn = DriverManager.getConnection(dbURL, name, password);
            System.out.println("连接数据库成功！");
        } catch (Exception e) {
            System.out.println("连接数据库失败！");
        }
    }

    public void forceLogout(String userID)   //广播消息到所有客户端
    {
        java.util.Date day=new Date();
        SimpleDateFormat sdf= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String time = sdf.format(day);
        Map<String,String> sendMessage = new HashMap<>();
        sendMessage.put("method", "5"); //5表示私聊
        sendMessage.put("sourceID", "10000");   //系统ID
        sendMessage.put("destID", "11111"); //广播ID
        sendMessage.put("msg", "用户(" + userID + ")被系统强制下线，请遵循聊天室规范");
        sendMessage.put("info", "forceLogout");
        sendMessage.put("targetID", userID);
        sendMessage.put("time", "系统消息 " +time);
        String jsonString = JSON.toJSONString(sendMessage);
        for (UserInfo v : Server.onlineUser.values()) {
            try {
                DataOutputStream out = new DataOutputStream(v.clientSocket.getOutputStream());
                out.writeUTF(jsonString);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void sendSystemMsg(String msg)   //广播消息到所有客户端
    {
        java.util.Date day=new Date();
        SimpleDateFormat sdf= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String time = sdf.format(day);
        Map<String,String> sendMessage = new HashMap<>();
        sendMessage.put("method", "5"); //5表示私聊
        sendMessage.put("sourceID", "10000");   //系统ID
        sendMessage.put("destID", "11111"); //广播ID
        sendMessage.put("msg", msg);
        sendMessage.put("time", "系统消息 " +time);
        String jsonString = JSON.toJSONString(sendMessage);
        for (UserInfo v : Server.onlineUser.values()) {
            try {
                DataOutputStream out = new DataOutputStream(v.clientSocket.getOutputStream());
                out.writeUTF(jsonString);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void run() {
        System.out.println("listening...");

        while (true)
        {
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("有客户端连接");

                new Thread(new UserServer(clientSocket)).start();  //与用户传输信息
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}

