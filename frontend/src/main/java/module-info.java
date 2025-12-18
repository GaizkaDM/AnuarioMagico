@SuppressWarnings("module") module org.GaizkaFrost {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires com.google.gson;
    requires java.logging;
    requires jasperreports;
    requires java.sql;
    requires org.slf4j;

    exports org.GaizkaFrost;
    exports org.GaizkaFrost.controllers;
    exports org.GaizkaFrost.models;
    exports org.GaizkaFrost.services;

    opens org.GaizkaFrost.controllers to javafx.fxml;
    opens org.GaizkaFrost to javafx.fxml;
}