module com.atmbanksimulator {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics; // <--- MISSING: This fixes the "module does not read it" error
    requires java.sql;
    requires java.desktop;
    requires mysql.connector.j;

    // We must open to graphics because Main.java extends Application
    opens com.atmbanksimulator to javafx.fxml, javafx.graphics;

    exports com.atmbanksimulator;
}