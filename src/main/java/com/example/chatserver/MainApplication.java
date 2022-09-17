package com.example.chatserver;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("main-view.fxml"));
        //fxmlloader通过首先调用默认构造函数，然后调用initialize方法
        Scene scene = new Scene(fxmlLoader.load(), 400, 300);
        stage.setTitle("聊天室服务器");
        stage.setOnCloseRequest(windowEvent -> {
            System.out.print("监听到窗口关闭");
            System.exit(0);
        });
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}