module org.example.fuelmanagement {
    requires transitive javafx.controls;
    requires transitive javafx.graphics;
    requires javafx.fxml;
    requires org.slf4j;
    requires com.zaxxer.hikari;
    requires java.sql;
    requires java.mail;
    requires java.desktop;
    requires java.net.http;
    requires io.github.cdimascio.dotenv.java;

    opens org.example.fuelmanagement to javafx.fxml;
    opens org.example.fuelmanagement.controller to javafx.fxml;
    opens org.example.fuelmanagement.controller.helper to javafx.fxml;
    opens org.example.fuelmanagement.controller.handler to javafx.fxml;
    opens org.example.fuelmanagement.dao to javafx.base;

    exports org.example.fuelmanagement;
    exports org.example.fuelmanagement.controller;
    exports org.example.fuelmanagement.controller.helper;
    exports org.example.fuelmanagement.controller.handler;
    exports org.example.fuelmanagement.model;
    exports org.example.fuelmanagement.model.enums;
    exports org.example.fuelmanagement.service;
    exports org.example.fuelmanagement.dao;
    exports org.example.fuelmanagement.util;
}