package com.example.chatserver;

import com.alibaba.fastjson.JSON;
import javafx.application.Platform;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class UserServer implements Runnable{
    private final Socket clientSocket;
    private final DataInputStream input;
    private final DataOutputStream output;
    private String userID;
    private String nickName;
    public UserServer(Socket socket) {
        this.clientSocket=socket;
        userID = "0";
        try {
            input = new DataInputStream(clientSocket.getInputStream());
            output = new DataOutputStream(clientSocket.getOutputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        boolean Running = true;
        while (Running) {
            try {
                String clientInputStr = input.readUTF();//这里要注意和客户端输出流的写方法对应,否则会抛 EOFException
                // 处理客户端数据
                System.out.println("客户端发过来的内容:" + clientInputStr);
                Map<String, String> maps = JSON.parseObject(clientInputStr, Map.class);

                switch (maps.get("method")) {
                    case "0" ->   //注册
                    {
                        int newID = createNewUser(maps.get("nickName"), maps.get("pwd"));
                        System.out.println(newID);
                        Map<String, String> sendMessage = new HashMap<>();
                        sendMessage.put("method", "01"); //01表示服务器返回注册结果
                        //sendMessage.put("state", "成功");
                        sendMessage.put("id", newID + "");

                        String jsonString = JSON.toJSONString(sendMessage);
                        output.writeUTF(jsonString);
                        this.clientSocket.close();  //关闭连接，线程结束
                        return;
                    }
                    case "1" ->   //登录
                    {
                        System.out.println("user login");
                        if (Server.onlineUser.containsKey(maps.get("userid")))   //用户已登录
                        {
                            Map<String, String> sendMessage = new HashMap<>();
                            sendMessage.put("method", "12"); //12表示服务器返回登录失败结果
                            String jsonString = JSON.toJSONString(sendMessage);
                            output.writeUTF(jsonString);
                            this.clientSocket.close();  //关闭连接，线程结束
                            return;
                        }

                        String select = "EXEC login '" + maps.get("userid") + "','" + maps.get("pwd") + "'";//简单查询语句
                        PreparedStatement statement = Server.dbConn.prepareStatement(select);
                        ResultSet res = statement.executeQuery();
                        if (res.next() && res.getString("num").equals("1"))    //账户密码匹配，登录成功
                        {
                            System.out.println("login succeed");
                            nickName = res.getString("nickName");
                            Map<String, String> sendMessage = new HashMap<>();
                            sendMessage.put("method", "11"); //11表示服务器返回登录成功结果
                            sendMessage.put("nickName", nickName);

                            String jsonString = JSON.toJSONString(sendMessage);
                            output.writeUTF(jsonString);

                            this.userID = maps.get("userid");
                            Server.onlineUser.put(this.userID, new UserInfo(res.getString("nickName"), this.clientSocket));
                            sendAllOnlineUser(); //返回所有在线用户到客户端
                            //广播该用户上线信息到其他客户端
                            String msg = "{\"method\":\"3\",\"nickName\":\"" + nickName + "\",\"userID\":\"" + this.userID + "\"}";
                            broadcastMsg(msg, maps.get("userid"));

                            //解决Not on FX application thread; currentThread = Thread-3
                            Platform.runLater(() -> {
                                Server.mainWindow.updateUserNum(++Server.userNum);//更新JavaFX的主线程的代码放在此处
                                Server.mainWindow.updateOnlineUserListView();
                            });
                            System.out.println(Server.onlineUser);
                            //Server.mainWindow.updateUserNum(Server.userNum);
                        } else {
                            System.out.println("login failed");
                            Map<String, String> sendMessage = new HashMap<>();
                            sendMessage.put("method", "10"); //10表示服务器返回注册登录失败结果
                            String jsonString = JSON.toJSONString(sendMessage);
                            output.writeUTF(jsonString);
                            this.clientSocket.close();  //关闭连接，线程结束
                            return;
                        }
                    }
                    case "2" ->   //注销
                    {
                        Running = false;
                        clientSocket.shutdownInput();
                        clientSocket.shutdownOutput();
                        clientSocket.close();
                        Server.onlineUser.remove(userID);

                        //广播用户下线信息
                        String msg = "{\"method\":\"4\",\"nickName\":\"" + nickName + "\",\"userID\":\"" + this.userID + "\"}";
                        broadcastMsg(msg);
                        //解决Not on FX application thread; currentThread = Thread-3
                        Platform.runLater(() -> {
                            Server.mainWindow.updateUserNum(--Server.userNum);//更新JavaFX的主线程的代码放在此处
                            Server.mainWindow.updateOnlineUserListView();
                        });
                    }
                    case "5" ->   //私聊
                    {
                        System.out.println("转发中...");
                        if (maps.get("destID").equals("11111"))  //表群发
                        {
                            broadcastMsg(clientInputStr, maps.get("sourceID"));
                        } else {
                            DataOutputStream output = new DataOutputStream(Server.onlineUser.get(maps.get("destID")).clientSocket.getOutputStream());
                            output.writeUTF(clientInputStr);    //转发到相应客户端
                        }
                    }
                }
            } catch (IOException e) {
                Running = false;
                e.printStackTrace();
            }//接收数据
            catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private int createNewUser(String nickName, String pwd) throws SQLException {
        int id = getNewUserID();
        String select = "EXEC createUser " + id +",'" + nickName + "','" + pwd +"'";//简单查询语句
        PreparedStatement statement = Server.dbConn.prepareStatement(select);
        System.out.println(select);
        statement.executeUpdate();//ResultSet res = statement.executeQuery();插入用这个会报错
        return id;
    }

    private int getNewUserID() throws SQLException {
        Random rd = new Random();
        int newID;
        String isExist = "1";
        do{
            newID = rd.nextInt(899999) + 100000;

            String select = "EXEC isIDexist " + newID;//简单查询语句
            PreparedStatement statement = Server.dbConn.prepareStatement(select);
            ResultSet res = statement.executeQuery();
            //当查询下一行有记录时：res.next()返回值为true，反之为false
            while (res.next()) {
                isExist = res.getString("num");
                //System.out.println(isExist);
            }
        }while (Objects.equals(isExist, "1"));
        return newID;
    }

    private void broadcastMsg(String jsonMsg)   //广播消息到所有客户端
    {
        for (UserInfo v : Server.onlineUser.values()) {
            try {
                DataOutputStream out = new DataOutputStream(v.clientSocket.getOutputStream());
                out.writeUTF(jsonMsg);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void broadcastMsg(String jsonMsg, String exceptUser)   //广播消息到所有客户端,除去特定用户
    {
        //获取iterator，iterator是一个迭代器，iterator.hasNext()用来判断是否还存在下一个entry，iterator.next()用来获取下一个entry
        for (Map.Entry<String, UserInfo> next : Server.onlineUser.entrySet()) {
            if (next.getKey().equals(exceptUser))
                continue;
            try {
                DataOutputStream out = new DataOutputStream(next.getValue().clientSocket.getOutputStream());
                out.writeUTF(jsonMsg);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            //System.out.println(next.getKey() + "==>" + next.getValue());
        }
    }

    private void sendAllOnlineUser()
    {
        for (Map.Entry<String, UserInfo> entry : Server.onlineUser.entrySet()) {
            Map<String, String> sendMessage = new HashMap<>();
            sendMessage.put("method", "3"); //3表示返回一个好友信息
            sendMessage.put("nickName", entry.getValue().nickName);
            sendMessage.put("userID", entry.getKey());

            String jsonString = JSON.toJSONString(sendMessage);
            try {
                output.writeUTF(jsonString);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            //System.out.println("key= " + entry.getKey() + " and value= " + entry.getValue());
        }

    }
}
