package com.atmbanksimulator;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Provides a single place for opening a connection to the MySQL database.
 * The class wraps {@code DriverManager.getConnection(...)} so the rest
 * of the application does not need to repeat connection logic.
 */
public class DBConnection {

    private static final boolean DEBUG = true;

    // Toggle this ON/OFF to show or hide the database inspector window.
    public static boolean DB_INSPECTOR_ENABLED = true;

    private static Stage inspectorStage;

    private static void debug(String msg) {
        if (DEBUG) {
            System.out.println(msg);
        }
    }

    private static final String URL =
            "jdbc:mysql://ia582.brighton.domains:3306/ia582_ATM-Simulator"
                    + "?useSSL=true&requireSSL=true&serverTimezone=UTC";

    private static final String USER = "ia582_Ibrahim";

    private static final String PASSWORD = "Project2026";

    /**
     * Opens and returns a new database connection.
     *
     * @return active {@code Connection} to the database
     * @throws SQLException if the connection attempt fails
     */
    public static Connection getConnection() throws SQLException {
        debug("DBConnection.getConnection() called");
        debug("URL = " + URL);
        debug("USER = " + USER);
        debug("Trying to connect...");

        try {
            Connection c = DriverManager.getConnection(URL, USER, PASSWORD);
            debug("CONNECTED TO MYSQL");
            debug("Connected to: " + c.getMetaData().getURL());
            return c;
        } catch (SQLException e) {
            debug("CONNECTION FAILED");
            debug("Message:   " + e.getMessage());
            debug("SQLState:  " + e.getSQLState());
            debug("ErrorCode: " + e.getErrorCode());
            throw e;
        }
    }

    /**
     * Opens the optional database inspector beside the main app window.
     */
    public static void launchInspector(Stage mainWindow) {
        if (!DB_INSPECTOR_ENABLED) {
            debug("DB inspector disabled.");
            return;
        }

        Platform.runLater(() -> {
            if (inspectorStage != null && inspectorStage.isShowing()) {
                inspectorStage.toFront();
                return;
            }

            DatabaseInspector inspector = new DatabaseInspector(mainWindow);
            inspectorStage = inspector.createStage();
            inspectorStage.show();
            inspector.loadSchema(true);
        });
    }

    /**
     * Manual test helper – run this once to verify credentials and network access.
     */
    public static void test() {
        debug("Running DBConnection.test()...");
        Connection c = null;
        try {
            c = getConnection();
            System.out.println("CONNECTED OK");
            System.out.println(c.getMetaData().getURL());
        } catch (SQLException e) {
            System.out.println("CONNECTION FAILED");
            System.out.println("Message:   " + e.getMessage());
            System.out.println("SQLState:  " + e.getSQLState());
            System.out.println("ErrorCode: " + e.getErrorCode());
            e.printStackTrace();
        } finally {
            if (c != null) {
                try {
                    debug("Closing connection...");
                    c.close();
                    debug("Connection closed.");
                } catch (SQLException e) {
                    System.out.println("Could not close connection.");
                }
            } else {
                debug("Nothing to close (connection was null).");
            }
        }
    }

    // ---------------------------------------------------------------------
    // Built-in mini SQL inspector / editor window
    // ---------------------------------------------------------------------
    private static class DatabaseInspector {
        private final Stage owner;

        private TextArea sqlInput;
        private TextArea schemaArea;
        private TextArea statusArea;
        private TableView<ObservableList<String>> resultTable;
        private Label connectionLabel;

        DatabaseInspector(Stage owner) {
            this.owner = owner;
        }

        Stage createStage() {
            Stage stage = new Stage();
            stage.setTitle("Database Inspector");
            stage.setWidth(900);
            stage.setHeight(620);

            if (owner != null) {
                stage.setX(owner.getX() + owner.getWidth() + 10);
                stage.setY(owner.getY());
            }

            connectionLabel = new Label("Database: " + URL);
            connectionLabel.setWrapText(true);

            sqlInput = new TextArea();
            sqlInput.setWrapText(true);
            sqlInput.setPrefRowCount(5);
            sqlInput.setPromptText(
                    "Write any SQL here...\n\n" +
                            "Examples:\n" +
                            "SELECT * FROM bank_accounts;\n" +
                            "SHOW TABLES;\n" +
                            "DESCRIBE bank_accounts;"
            );

            Button runBtn = new Button("Run SQL");
            runBtn.setOnAction(e -> runSql());

            Button schemaBtn = new Button("Refresh Schema");
            schemaBtn.setOnAction(e -> loadSchema(true));

            Button clearBtn = new Button("Clear Output");
            clearBtn.setOnAction(e -> {
                resultTable.getColumns().clear();
                resultTable.getItems().clear();
                statusArea.clear();
            });

            Button sampleBtn = new Button("Insert Sample Query");
            sampleBtn.setOnAction(e -> sqlInput.setText("SELECT * FROM bank_accounts LIMIT 50;"));

            HBox topButtons = new HBox(10, runBtn, schemaBtn, clearBtn, sampleBtn);

            VBox topArea = new VBox(10,
                    new Label("SQL Editor"),
                    connectionLabel,
                    sqlInput,
                    topButtons
            );
            topArea.setPadding(new Insets(10));

            schemaArea = new TextArea();
            schemaArea.setEditable(false);
            schemaArea.setWrapText(false);

            resultTable = new TableView<>();
            resultTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

            statusArea = new TextArea();
            statusArea.setEditable(false);
            statusArea.setPrefRowCount(7);
            statusArea.setWrapText(true);

            VBox rightPane = new VBox(10,
                    new Label("Query Results"),
                    resultTable,
                    new Label("Status / Messages"),
                    statusArea
            );
            rightPane.setPadding(new Insets(10));
            VBox.setVgrow(resultTable, Priority.ALWAYS);

            VBox leftPane = new VBox(10,
                    new Label("Database Structure"),
                    schemaArea
            );
            leftPane.setPadding(new Insets(10));
            VBox.setVgrow(schemaArea, Priority.ALWAYS);

            SplitPane splitPane = new SplitPane(leftPane, rightPane);
            splitPane.setOrientation(Orientation.HORIZONTAL);
            splitPane.setDividerPositions(0.42);

            BorderPane root = new BorderPane();
            root.setTop(topArea);
            root.setCenter(splitPane);

            Scene scene = new Scene(root);
            stage.setScene(scene);
            return stage;
        }

        void loadSchema(boolean updateStatusMessage) {
            StringBuilder sb = new StringBuilder();

            try (Connection conn = getConnection()) {
                DatabaseMetaData meta = conn.getMetaData();

                sb.append("DATABASE INSPECTOR\n");
                sb.append("==================================================\n");
                sb.append("URL: ").append(meta.getURL()).append("\n");
                sb.append("User: ").append(meta.getUserName()).append("\n");
                sb.append("Product: ").append(meta.getDatabaseProductName())
                        .append(" ").append(meta.getDatabaseProductVersion()).append("\n");
                sb.append("Driver: ").append(meta.getDriverName())
                        .append(" ").append(meta.getDriverVersion()).append("\n\n");

                sb.append("TABLES / COLUMNS\n");
                sb.append("==================================================\n");

                try (ResultSet tables = meta.getTables(conn.getCatalog(), null, "%", new String[]{"TABLE"})) {
                    boolean foundTable = false;

                    while (tables.next()) {
                        foundTable = true;
                        String tableName = tables.getString("TABLE_NAME");

                        sb.append("\nTABLE: ").append(tableName).append("\n");
                        sb.append("--------------------------------------------------\n");

                        try (ResultSet columns = meta.getColumns(conn.getCatalog(), null, tableName, "%")) {
                            while (columns.next()) {
                                sb.append(" - ")
                                        .append(columns.getString("COLUMN_NAME"))
                                        .append(" | ")
                                        .append(columns.getString("TYPE_NAME"));

                                int size = columns.getInt("COLUMN_SIZE");
                                if (size > 0) {
                                    sb.append("(").append(size).append(")");
                                }

                                String nullable = columns.getInt("NULLABLE") == DatabaseMetaData.columnNullable
                                        ? "NULL"
                                        : "NOT NULL";
                                sb.append(" | ").append(nullable);

                                String defaultValue = columns.getString("COLUMN_DEF");
                                if (defaultValue != null) {
                                    sb.append(" | DEFAULT=").append(defaultValue);
                                }
                                sb.append("\n");
                            }
                        }

                        try (ResultSet primaryKeys = meta.getPrimaryKeys(conn.getCatalog(), null, tableName)) {
                            StringBuilder pk = new StringBuilder();
                            while (primaryKeys.next()) {
                                if (pk.length() > 0) pk.append(", ");
                                pk.append(primaryKeys.getString("COLUMN_NAME"));
                            }
                            if (pk.length() > 0) {
                                sb.append(" PRIMARY KEY: ").append(pk).append("\n");
                            }
                        }
                    }

                    if (!foundTable) {
                        sb.append("No tables found.\n");
                    }
                }

                schemaArea.setText(sb.toString());
                if (updateStatusMessage) {
                    statusArea.setText("Schema loaded successfully.\nYou can now run any SQL in the editor above.");
                }
            } catch (SQLException e) {
                schemaArea.setText("Could not load schema.\n\n" + e.getMessage());
                if (updateStatusMessage) {
                    statusArea.setText(buildSQLExceptionMessage(e));
                }
            }
        }

        private void runSql() {
            String sql = sqlInput.getText().trim();
            if (sql.isEmpty()) {
                statusArea.setText("Please enter SQL first.");
                return;
            }

            resultTable.getColumns().clear();
            resultTable.getItems().clear();

            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement()) {

                boolean hasResultSet = stmt.execute(sql);

                if (hasResultSet) {
                    try (ResultSet rs = stmt.getResultSet()) {
                        populateResultTable(rs);
                    }
                } else {
                    int rows = stmt.getUpdateCount();
                    statusArea.setText(
                            "SQL executed successfully.\n" +
                                    "Rows affected: " + rows + "\n\n" +
                                    "Query:\n" + sql
                    );
                }

                loadSchema(false);
            } catch (SQLException e) {
                statusArea.setText(buildSQLExceptionMessage(e));
            }
        }

        private void populateResultTable(ResultSet rs) throws SQLException {
            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();

            for (int i = 1; i <= columnCount; i++) {
                final int columnIndex = i - 1;
                String columnTitle = meta.getColumnLabel(i) + " (" + meta.getColumnTypeName(i) + ")";

                TableColumn<ObservableList<String>, String> column = new TableColumn<>(columnTitle);
                column.setPrefWidth(150);
                column.setCellValueFactory(cellData -> {
                    ObservableList<String> row = cellData.getValue();
                    String value = columnIndex < row.size() ? row.get(columnIndex) : "";
                    return new ReadOnlyStringWrapper(value);
                });
                resultTable.getColumns().add(column);
            }

            ObservableList<ObservableList<String>> data = FXCollections.observableArrayList();
            int rowCount = 0;

            while (rs.next()) {
                ObservableList<String> row = FXCollections.observableArrayList();
                for (int i = 1; i <= columnCount; i++) {
                    Object value = rs.getObject(i);
                    row.add(value == null ? "NULL" : String.valueOf(value));
                }
                data.add(row);
                rowCount++;
            }

            resultTable.setItems(data);
            statusArea.setText("Query executed successfully.\nRows returned: " + rowCount);
        }

        private String buildSQLExceptionMessage(SQLException e) {
            return "SQL ERROR\n" +
                    "Message: " + e.getMessage() + "\n" +
                    "SQLState: " + e.getSQLState() + "\n" +
                    "ErrorCode: " + e.getErrorCode();
        }
    }
}
