package com.atmbanksimulator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

// ===== Bank (Service / Business Logic) =====
// Handles all DB operations and delegates business rules to account subclasses.
public class Bank {

    private BankAccount loggedInAccount = null;

    // In-memory map to track failed login attempts.
    private Map<String, Integer> loginFailures = new HashMap<>();

    // -----------------------------------------------------------------------
    // Factory – creates the correct account subclass based on type string
    // -----------------------------------------------------------------------
    public BankAccount makeBankAccount(String accNumber, String accPasswd, int balance, String type) {
        switch (type) {
            case "student":
                return new StudentAccount(accNumber, accPasswd, balance);
            case "prime":
                return new PrimeAccount(accNumber, accPasswd, balance);
            case "saving":
                return new SavingAccount(accNumber, accPasswd, balance);
            default:
                return new BankAccount(accNumber, accPasswd, balance);
        }
    }

    public BankAccount makeBankAccount(String accNumber, String accPasswd, int balance) {
        return makeBankAccount(accNumber, accPasswd, balance, "standard");
    }

    // -----------------------------------------------------------------------
    // addBankAccount – inserts a new account into the database
    // -----------------------------------------------------------------------
    public boolean addBankAccount(BankAccount a) {
        String sql = "INSERT IGNORE INTO bank_accounts "
                + "(acc_number, acc_password, balance, account_type) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, a.getAccNumber());
            ps.setString(2, a.getaccPasswd());
            ps.setInt(3, a.getBalance());
            ps.setString(4, a.getAccountType());
            ps.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.out.println("Bank.addBankAccount failed: " + e.getMessage());
            return false;
        }
    }

    public boolean addBankAccount(String accNumber, String accPasswd, int balance, String type) {
        return addBankAccount(makeBankAccount(accNumber, accPasswd, balance, type));
    }

    // -----------------------------------------------------------------------
    // login – includes account locking logic (3 attempts).
    // -----------------------------------------------------------------------
    public boolean login(String accountNumber, String password) {
        logout();

        if (loginFailures.getOrDefault(accountNumber, 0) >= 3) {
            System.out.println("Login blocked: Account " + accountNumber + " is locked.");
            return false;
        }

        String sql = "SELECT acc_number, acc_password, balance, account_type "
                + "FROM bank_accounts WHERE acc_number = ? AND acc_password = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, accountNumber);
            ps.setString(2, password);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    loginFailures.put(accountNumber, 0);
                    String accNum = rs.getString("acc_number");
                    String accPwd = rs.getString("acc_password");
                    int bal = rs.getInt("balance");
                    String type = rs.getString("account_type");

                    loggedInAccount = makeBankAccount(accNum, accPwd, bal, type);
                    return true;
                } else {
                    int currentFailures = loginFailures.getOrDefault(accountNumber, 0);
                    loginFailures.put(accountNumber, currentFailures + 1);
                }
            }
        } catch (SQLException e) {
            System.out.println("Bank.login failed: " + e.getMessage());
        }
        loggedInAccount = null;
        return false;
    }

    public boolean isLocked(String accNum) {
        return loginFailures.getOrDefault(accNum, 0) >= 3;
    }

    public void logout() {
        loggedInAccount = null;
    }

    public boolean loggedIn() {
        return loggedInAccount != null;
    }

    private int fetchLiveBalance() {
        String sql = "SELECT balance FROM bank_accounts WHERE acc_number = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, loggedInAccount.getAccNumber());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("balance");
            }
        } catch (SQLException e) {
            System.out.println("Bank.fetchLiveBalance failed: " + e.getMessage());
        }
        return -1;
    }

    public boolean deposit(int amount) {
        if (!loggedIn()) return false;
        int liveBalance = fetchLiveBalance();
        if (liveBalance == -1) return false;
        loggedInAccount.balance = liveBalance;

        if (!loggedInAccount.deposit(amount)) return false;

        int newBalance = loggedInAccount.getBalance();
        String sql = "UPDATE bank_accounts SET balance = ? WHERE acc_number = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, newBalance);
            ps.setString(2, loggedInAccount.getAccNumber());
            ps.executeUpdate();
            TransactionLogger.log(loggedInAccount.getAccNumber(), "DEPOSIT", amount, newBalance);
            return true;
        } catch (SQLException e) {
            loggedInAccount.balance = liveBalance;
            return false;
        }
    }

    // -----------------------------------------------------------------------
    // withdraw – FIXED: Returns String to explain why it failed.
    // -----------------------------------------------------------------------
    public String withdraw(int amount) {
        if (!loggedIn()) return "NOT_LOGGED_IN";
        if (amount <= 0) return "INVALID_AMOUNT";

        // Sync live balance
        int liveBalance = fetchLiveBalance();
        if (liveBalance == -1) return "DB_ERROR";
        loggedInAccount.balance = liveBalance;

        // Check Student Account Limit specifically to give better feedback
        if (loggedInAccount instanceof StudentAccount) {
            StudentAccount sa = (StudentAccount) loggedInAccount;
            if (amount > sa.getRemainingDailyLimit()) {
                return "DAILY_LIMIT_REACHED";
            }
        }

        // Delegate to subclass logic (Insufficient funds check happens here)
        if (!loggedInAccount.withdraw(amount)) {
            return "INSUFFICIENT_FUNDS";
        }

        int newBalance = loggedInAccount.getBalance();
        String sql = "UPDATE bank_accounts SET balance = ? WHERE acc_number = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, newBalance);
            ps.setString(2, loggedInAccount.getAccNumber());
            ps.executeUpdate();
            TransactionLogger.log(loggedInAccount.getAccNumber(), "WITHDRAW", amount, newBalance);
            return "SUCCESS";

        } catch (SQLException e) {
            System.out.println("Bank.withdraw DB update failed: " + e.getMessage());
            loggedInAccount.balance = liveBalance; // rollback in-memory
            return "DB_UPDATE_FAILED";
        }
    }

    // -----------------------------------------------------------------------
    // transfer – moves money from current account to another
    // -----------------------------------------------------------------------
    public boolean transfer(String toAccNum, int amount) {
        if (!loggedIn() || amount <= 0) return false;
        if (toAccNum.equals(loggedInAccount.getAccNumber())) return false;

        // 1. Attempt to withdraw from the sender (Checking SUCCESS string)
        if (!withdraw(amount).equals("SUCCESS")) return false;

        // 2. Attempt to update the recipient's balance
        String sql = "UPDATE bank_accounts SET balance = balance + ? WHERE acc_number = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, amount);
            ps.setString(2, toAccNum);
            int rowsAffected = ps.executeUpdate();

            if (rowsAffected == 0) {
                deposit(amount); // Rollback
                return false;
            }
            return true;
        } catch (SQLException e) {
            deposit(amount); // Rollback
            return false;
        }
    }

    public int getBalance() {
        if (!loggedIn()) return -1;
        String sql = "SELECT balance FROM bank_accounts WHERE acc_number = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, loggedInAccount.getAccNumber());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("balance");
            }
        } catch (SQLException e) {
            System.out.println("Bank.getBalance failed: " + e.getMessage());
        }
        return -1;
    }

    public String getAccountType() {
        if (!loggedIn()) return "none";
        return loggedInAccount.getAccountType();
    }

    public boolean changePassword(String newPass) {
        if (!loggedIn()) return false;
        if (newPass.length() < 6) return false;
        if (!newPass.matches(".*[A-Za-z].*") || !newPass.matches(".*[0-9].*")) return false;

        String sql = "UPDATE bank_accounts SET acc_password = ? WHERE acc_number = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newPass);
            ps.setString(2, loggedInAccount.getAccNumber());
            int rows = ps.executeUpdate();
            if (rows > 0) {
                loggedInAccount.accPasswd = newPass;
                return true;
            }
            return false;
        } catch (SQLException e) {
            return false;
        }
    }

    public String getLoggedInPassword() {
        if (!loggedIn()) return "";
        return loggedInAccount.getaccPasswd();
    }

    public List<String> getMiniStatement() {
        if (!loggedIn()) return new java.util.ArrayList<>();
        return TransactionLogger.getRecent(loggedInAccount.getAccNumber(), 5);
    }

    public boolean isLowBalance() {
        return loggedIn() && getBalance() < 50;
    }
}