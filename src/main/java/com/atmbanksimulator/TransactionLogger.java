package com.atmbanksimulator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TransactionLogger {

    public static void log(String accNumber, String type, int amount, int balanceAfter) {
        String sql = "INSERT INTO transactions (acc_number, type, amount, balance_after) " +
                "VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, accNumber);
            ps.setString(2, type);
            ps.setInt(3, amount);
            ps.setInt(4, balanceAfter);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("TransactionLogger.log failed: " + e.getMessage());
        }
    }

    public static List<String> getRecent(String accNumber, int limit) {
        List<String> lines = new ArrayList<>();
        String sql = "SELECT type, amount, balance_after, created_at " +
                "FROM transactions WHERE acc_number = ? " +
                "ORDER BY created_at DESC LIMIT ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, accNumber);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String type  = rs.getString("type");
                    int amount   = rs.getInt("amount");
                    int balAfter = rs.getInt("balance_after");
                    String time  = rs.getString("created_at").substring(0, 16);
                    lines.add(type + " \u00A3" + amount +
                            " | Bal: \u00A3" + balAfter +
                            "\n    " + time);
                }
            }
        } catch (SQLException e) {
            System.out.println("TransactionLogger.getRecent failed: " + e.getMessage());
        }
        return lines;
    }
}