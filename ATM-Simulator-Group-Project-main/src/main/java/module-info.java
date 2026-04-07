module com.atmbanksimulator {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires mysql.connector.j;
    requires java.desktop;

    opens com.atmbanksimulator to javafx.fxml;
    exports com.atmbanksimulator;
}