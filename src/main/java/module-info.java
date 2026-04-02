module com.atmbanksimulator {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.atmbanksimulator to javafx.fxml;
    exports com.atmbanksimulator;
}