package com.atmbanksimulator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

// ===== Bank (Service / Business Logic) =====
// Handles all DB operations and delegates business rules to account subclasses.
public class Bank {

    private BankAccount loggedInAccount = null;

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

    // Convenience overload – defaults to standard account
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

    public boolean addBankAccount(String accNumber, String accPasswd, int balance) {
        return addBankAccount(accNumber, accPasswd, balance, "standard");
    }

    // -----------------------------------------------------------------------
    // login – reads account_type and creates the correct subclass
    // -----------------------------------------------------------------------
    public boolean login(String accountNumber, String password) {
        logout();

        String sql = "SELECT acc_number, acc_password, balance, account_type "
                + "FROM bank_accounts WHERE acc_number = ? AND acc_password = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, accountNumber);
            ps.setString(2, password);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String accNum = rs.getString("acc_number");
                    String accPwd = rs.getString("acc_password");
                    int bal = rs.getInt("balance");
                    String type = rs.getString("account_type");

                    // Create the right subclass based on account_type in DB
                    loggedInAccount = makeBankAccount(accNum, accPwd, bal, type);
                    return true;
                }
            }

        } catch (SQLException e) {
            System.out.println("Bank.login failed: " + e.getMessage());
        }

        loggedInAccount = null;
        return false;
    }

    // -----------------------------------------------------------------------
    // logout / loggedIn helpers
    // -----------------------------------------------------------------------
    public void logout() {
        loggedInAccount = null;
    }

    public boolean loggedIn() {
        return loggedInAccount != null;
    }

    // -----------------------------------------------------------------------
    // fetchLiveBalance – shared helper to get current balance from DB
    // -----------------------------------------------------------------------
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

    // -----------------------------------------------------------------------
    // deposit – syncs live balance, delegates to subclass, then updates DB.
    // SavingAccount.deposit() will automatically add interest.
    // -----------------------------------------------------------------------
    public boolean deposit(int amount) {
        if (!loggedIn()) return false;

        // Sync live balance from DB into the in-memory object
        int liveBalance = fetchLiveBalance();
        if (liveBalance == -1) return false;
        loggedInAccount.balance = liveBalance;

        // Delegate to subclass (e.g. SavingAccount adds interest here)
        if (!loggedInAccount.deposit(amount)) return false;

        int newBalance = loggedInAccount.getBalance();

        String sql = "UPDATE bank_accounts SET balance = ? WHERE acc_number = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, newBalance);
            ps.setString(2, loggedInAccount.getAccNumber());
            ps.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.out.println("Bank.deposit DB update failed: " + e.getMessage());
            loggedInAccount.balance = liveBalance; // rollback in-memory
            return false;
        }
    }

    // -----------------------------------------------------------------------
    // withdraw – syncs live balance, delegates to subclass, then updates DB.
    // StudentAccount enforces daily cap, PrimeAccount allows overdraft.
    // -----------------------------------------------------------------------
    public boolean withdraw(int amount) {
        if (!loggedIn()) return false;
        if (amount <= 0) return false;

        // Sync live balance from DB into the in-memory object
        int liveBalance = fetchLiveBalance();
        if (liveBalance == -1) return false;
        loggedInAccount.balance = liveBalance;

        // Delegate to subclass (applies specific rules per account type)
        if (!loggedInAccount.withdraw(amount)) return false;

        int newBalance = loggedInAccount.getBalance();

        String sql = "UPDATE bank_accounts SET balance = ? WHERE acc_number = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, newBalance);
            ps.setString(2, loggedInAccount.getAccNumber());
            ps.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.out.println("Bank.withdraw DB update failed: " + e.getMessage());
            loggedInAccount.balance = liveBalance; // rollback in-memory
            return false;
        }
    }

    // -----------------------------------------------------------------------
    // getBalance – always fetches live value from DB
    // -----------------------------------------------------------------------
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

    // Returns the account type of the logged-in user (useful for UI display)
    public String getAccountType() {
        if (!loggedIn()) return "none";
        return loggedInAccount.getAccountType();
    }


// changePassword – verifies old password then updates to new one atomically
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
                loggedInAccount.accPasswd = newPass; // keep in-memory in sync
                return true;
            }
            return false;
        } catch (SQLException e) {
            System.out.println("Bank.changePassword failed: " + e.getMessage());
            return false;
        }
    }
    public String getLoggedInPassword() {
        if (!loggedIn()) return "";
        return loggedInAccount.getaccPasswd();
    }
}