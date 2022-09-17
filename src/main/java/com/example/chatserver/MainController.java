package com.example.chatserver;

import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;

import java.util.Map;

public class MainController {
    public Label ipLabel;
    public Label portLabel;
    public Label userNumLabel;
    public Button startButton;
    public Label infoLabel;
    public ListView onlineUserListView;
    public Button logoutUserButton;
    public TextArea systemMsg;
    public Button sendButton;

    Server chatServer;
    String selectUser;


    public MainController()
    {
        selectUser = null;
    }

    @FXML
    public void initialize()
    {
        onlineUserListView.getSelectionModel().selectedItemProperty().addListener(
                (ChangeListener<Object>) (observable, oldValue, newValue) ->
                        selectUser = (String)newValue);
    }

    public void updateWindow(String ip, int port)
    {
        this.ipLabel.setText(ip);
        this.portLabel.setText(port + "");
    }

    public void updateUserNum(int num)
    {
        this.userNumLabel.setText(num+"");
    }
    public void clickStartButton() {
        chatServer = new Server(8888, this);
        this.infoLabel.setText("服务器已启动");
        this.startButton.setVisible(false);
    }

    public void updateOnlineUserListView()
    {
        ObservableList<String> list = FXCollections.observableArrayList();
        for (Map.Entry<String, UserInfo> next : Server.onlineUser.entrySet()) {
            list.add(next.getKey() + "(" + next.getValue().nickName + ")");
            //System.out.println(next.getKey() + "==>" + next.getValue());
        }
        onlineUserListView.setItems(list);
    }

    public void clickLogoutUserButton() {
        if(selectUser == null)
        {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("错误");
            alert.setHeaderText(null);
            alert.setContentText("请选择要强制下线的用户");
            alert.showAndWait();
        }
        else
        {
            String userID = selectUser.substring(0, 6);
            chatServer.forceLogout(userID);
        }
    }

    public void clickSendButton() {
        chatServer.sendSystemMsg(systemMsg.getText());
        systemMsg.clear();
    }
}