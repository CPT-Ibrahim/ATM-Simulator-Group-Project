package com.atmbanksimulator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

// ===== 📚🌐Bank (Domain / Service / Business Logic) =====
//
// Bank class: all account data is now stored in and read from the MySQL database.
// The only in-memory state kept is the currently logged-in BankAccount object,
// which is used as a session token (account number, password, and a local
// balance snapshot). Every deposit / withdrawal / balance query is executed
// directly against the database so data is always consistent.

public class Bank {

    // Currently logged-in account object (null when no one is logged in).
    // This is a lightweight in-memory object that mirrors the DB row for the
    // active session; it is discarded on logout.
    private BankAccount loggedInAccount = null;

    // -----------------------------------------------------------------------
    // Factory / helper
    // -----------------------------------------------------------------------

    // Factory method – keeps the rest of the code from needing 'new BankAccount' directly.
    public BankAccount makeBankAccount(String accNumber, String accPasswd, int balance) {
        return new BankAccount(accNumber, accPasswd, balance);
    }

    // -----------------------------------------------------------------------
    // addBankAccount – inserts a new account into the database.
    // Uses INSERT IGNORE so calling it again with the same account number
    // is safe (silently does nothing instead of throwing a duplicate-key error).
    // -----------------------------------------------------------------------

    public boolean addBankAccount(BankAccount a) {
        String sql = "INSERT IGNORE INTO bank_accounts (acc_number, acc_password, balance) "
                   + "VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, a.getAccNumber());
            ps.setString(2, a.getaccPasswd());
            ps.setInt   (3, a.getBalance());
            ps.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.out.println("Bank.addBankAccount failed: " + e.getMessage());
            return false;
        }
    }

    // Overloaded convenience version – creates a BankAccount and inserts it in one step.
    public boolean addBankAccount(String accNumber, String accPasswd, int balance) {
        return addBankAccount(makeBankAccount(accNumber, accPasswd, balance));
    }

    // -----------------------------------------------------------------------
    // login – look up matching row in the database; keep object in memory for session.
    // -----------------------------------------------------------------------

    public boolean login(String accountNumber, String password) {
        logout(); // clear any previous session first

        String sql = "SELECT acc_number, acc_password, balance "
                   + "FROM bank_accounts "
                   + "WHERE acc_number = ? AND acc_password = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, accountNumber);
            ps.setString(2, password);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Row found – create in-memory session object
                    loggedInAccount = new BankAccount(
                        rs.getString("acc_number"),
                        rs.getString("acc_password"),
                        rs.getInt   ("balance")
                    );
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
    // deposit – adds 'amount' to the balance column in the database.
    // Also updates the local session object so the in-memory state stays in sync.
    // -----------------------------------------------------------------------

    public boolean deposit(int amount) {
        if (!loggedIn()) return false;

        // Delegate validation to BankAccount (amount must not be negative)
        if (!loggedInAccount.deposit(amount)) return false;

        String sql = "UPDATE bank_accounts SET balance = balance + ? WHERE acc_number = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt   (1, amount);
            ps.setString(2, loggedInAccount.getAccNumber());
            ps.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.out.println("Bank.deposit DB update failed: " + e.getMessage());
            // Roll back the in-memory change so local state stays consistent
            loggedInAccount.withdraw(amount);
            return false;
        }
    }

    // -----------------------------------------------------------------------
    // withdraw – reads the live balance from the DB first, then deducts.
    // This prevents over-drafts in case the balance changed since login.
    // -----------------------------------------------------------------------

    public boolean withdraw(int amount) {
        if (!loggedIn()) return false;
        if (amount <= 0)  return false;

        // Fetch live balance from the database
        String selectSql = "SELECT balance FROM bank_accounts WHERE acc_number = ?";
        int liveBalance;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(selectSql)) {

            ps.setString(1, loggedInAccount.getAccNumber());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return false;  // account somehow missing
                liveBalance = rs.getInt("balance");
            }

        } catch (SQLException e) {
            System.out.println("Bank.withdraw balance check failed: " + e.getMessage());
            return false;
        }

        // Reject if insufficient funds
        if (liveBalance < amount) return false;

        // Perform the deduction in the database
        String updateSql = "UPDATE bank_accounts SET balance = balance - ? WHERE acc_number = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(updateSql)) {

            ps.setInt   (1, amount);
            ps.setString(2, loggedInAccount.getAccNumber());
            ps.executeUpdate();

            // Keep the in-memory session object in sync
            loggedInAccount.withdraw(amount);
            return true;

        } catch (SQLException e) {
            System.out.println("Bank.withdraw DB update failed: " + e.getMessage());
            return false;
        }
    }

    // -----------------------------------------------------------------------
    // getBalance – always fetches the live value from the database.
    // -----------------------------------------------------------------------

    public int getBalance() {
        if (!loggedIn()) return -1;

        String sql = "SELECT balance FROM bank_accounts WHERE acc_number = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, loggedInAccount.getAccNumber());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("balance");
                }
            }

        } catch (SQLException e) {
            System.out.println("Bank.getBalance failed: " + e.getMessage());
        }

        return -1; // -1 indicates an error
    }
}
