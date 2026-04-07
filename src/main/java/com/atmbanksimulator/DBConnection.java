package com.atmbanksimulator;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Provides a single place for opening a connection to the MySQL database.
 * The class wraps {@code DriverManager.getConnection(...)} so the rest
 * of the application does not need to repeat connection logic.
 */
public class DBConnection {

    private static final boolean DEBUG = true;

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
}
