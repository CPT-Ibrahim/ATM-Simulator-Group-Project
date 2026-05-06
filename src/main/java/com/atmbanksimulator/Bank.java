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
            case "student": return new StudentAccount(accNumber, accPasswd, balance);
            case "prime":   return new PrimeAccount(accNumber, accPasswd, balance);
            case "saving":  return new SavingAccount(accNumber, accPasswd, balance);
            default:        return new BankAccount(accNumber, accPasswd, balance);
        }
    }

    public BankAccount makeBankAccount(String accNumber, String accPasswd, int balance) {
        return makeBankAccount(accNumber, accPasswd, balance, "standard");
    }

    // -----------------------------------------------------------------------
    // addBankAccount – inserts a new account into the database
    // -----------------------------------------------------------------------
    public boolean addBankAccount(BankAccount a) {
        // nfc_uid is NOT NULL and has a unique index, so we can't use NULL or ''.
        // We store a unique placeholder "MANUAL_<accNumber>" for accounts created
        // without an NFC card. This satisfies both constraints and can be
        // overwritten later when a card is linked to the account.
        String placeholder = "MANUAL_" + a.getAccNumber();
        String sql = "INSERT INTO bank_accounts "
                + "(acc_number, acc_password, balance, account_type, nfc_uid) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, a.getAccNumber());
            ps.setString(2, a.getaccPasswd());
            ps.setInt(3, a.getBalance());
            ps.setString(4, a.getAccountType());
            ps.setString(5, placeholder);
            int rows = ps.executeUpdate();
            return rows > 0;
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
    // loginByUID – authenticates by NFC card UID instead of
    // account number + password. Called by NFCServer when a card is tapped.
    // -----------------------------------------------------------------------
    public boolean loginByUID(String uid) {
        logout();

        String sql = "SELECT acc_number, acc_password, balance, account_type "
                + "FROM bank_accounts WHERE nfc_uid = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, uid.trim().toUpperCase());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    loggedInAccount = makeBankAccount(
                            rs.getString("acc_number"),
                            rs.getString("acc_password"),
                            rs.getInt("balance"),
                            rs.getString("account_type")
                    );
                    System.out.println("[Bank] NFC login success — account: "
                            + loggedInAccount.getAccNumber()
                            + " (" + loggedInAccount.getAccountType() + ")");
                    return true;
                } else {
                    System.out.println("[Bank] NFC login failed — UID not found: " + uid);
                }
            }

        } catch (SQLException e) {
            System.out.println("Bank.loginByUID failed: " + e.getMessage());
        }

        loggedInAccount = null;
        return false;
    }

    // -----------------------------------------------------------------------
    // login – original method (account number + password)
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
                    loggedInAccount = makeBankAccount(
                            rs.getString("acc_number"),
                            rs.getString("acc_password"),
                            rs.getInt("balance"),
                            rs.getString("account_type")
                    );
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

    // -----------------------------------------------------------------------
    // logout / loggedIn helpers
    // -----------------------------------------------------------------------
    public void logout() { loggedInAccount = null; }
    public boolean loggedIn() { return loggedInAccount != null; }

    // -----------------------------------------------------------------------
    // fetchLiveBalance
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
    // deposit
    // -----------------------------------------------------------------------
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
            System.out.println("Bank.deposit DB update failed: " + e.getMessage());
            loggedInAccount.balance = liveBalance;
            return false;
        }
    }

    // -----------------------------------------------------------------------
    // withdraw
    // -----------------------------------------------------------------------
    public boolean withdraw(int amount) {
        if (!loggedIn()) return false;
        if (amount <= 0) return false;
        int liveBalance = fetchLiveBalance();
        if (liveBalance == -1) return false;
        loggedInAccount.balance = liveBalance;
        if (!loggedInAccount.withdraw(amount)) return false;
        int newBalance = loggedInAccount.getBalance();
        String sql = "UPDATE bank_accounts SET balance = ? WHERE acc_number = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, newBalance);
            ps.setString(2, loggedInAccount.getAccNumber());
            ps.executeUpdate();
            TransactionLogger.log(loggedInAccount.getAccNumber(), "WITHDRAW", amount, newBalance);
            return true;
        } catch (SQLException e) {
            System.out.println("Bank.withdraw DB update failed: " + e.getMessage());
            loggedInAccount.balance = liveBalance;
            return false;
        }
    }

    // -----------------------------------------------------------------------
    // transfer
    // -----------------------------------------------------------------------
    public boolean transfer(String toAccNum, int amount) {
        if (!loggedIn() || amount <= 0) return false;
        if (toAccNum.equals(loggedInAccount.getAccNumber())) return false;
        if (!withdraw(amount)) return false;
        String sql = "UPDATE bank_accounts SET balance = balance + ? WHERE acc_number = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, amount);
            ps.setString(2, toAccNum);
            int rowsAffected = ps.executeUpdate();
            if (rowsAffected == 0) {
                deposit(amount);
                return false;
            }
            return true;
        } catch (SQLException e) {
            System.out.println("Bank.transfer failed: " + e.getMessage());
            deposit(amount);
            return false;
        }
    }

    // -----------------------------------------------------------------------
    // getBalance
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

    public String getAccountType() {
        if (!loggedIn()) return "none";
        return loggedInAccount.getAccountType();
    }

    // -----------------------------------------------------------------------
    // changePassword
    // -----------------------------------------------------------------------
    public boolean changePassword(String newPass) {
        if (!loggedIn()) return false;
        if (newPass == null || !newPass.matches("\\d{4,}")) return false;
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
            System.out.println("Bank.changePassword failed: " + e.getMessage());
            return false;
        }
    }

    public String getLoggedInPassword() {
        if (!loggedIn()) return "";
        return loggedInAccount.getaccPasswd();
    }

    // -----------------------------------------------------------------------
    // getMiniStatement
    // -----------------------------------------------------------------------
    public List<String> getMiniStatement() {
        if (!loggedIn()) return new java.util.ArrayList<>();
        return TransactionLogger.getRecent(loggedInAccount.getAccNumber(), 5);
    }

    // -----------------------------------------------------------------------
    // isLowBalance
    // -----------------------------------------------------------------------
    public boolean isLowBalance() {
        return loggedIn() && getBalance() < 50;
    }
}