module org.diegofg {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires com.google.gson;

    opens org.GaizkaFrost to javafx.fxml;

    exports org.GaizkaFrost;
}