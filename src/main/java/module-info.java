module com.example.chatserver {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires fastjson;
    requires java.sql;
    requires com.microsoft.sqlserver.jdbc;

    opens com.example.chatserver to javafx.fxml;
    exports com.example.chatserver;
}