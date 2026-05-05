package com.atmbanksimulator;

// ===== BankAccount (Base Class) =====
// Base class for all account types.
// Fields are protected so subclasses can access them directly.
public class BankAccount {

    protected String accNumber  = "";
    protected String accPasswd  = "";
    protected int    balance    = 0;
    protected String accountType = "standard";

    // Define the maximum allowed balance for any account
    protected static final int MAX_BALANCE = 1_000_000_000; // 1 Billion

    public BankAccount() {}

    public BankAccount(String a, String p, int b) {
        accNumber   = a;
        accPasswd   = p;
        balance     = b;
        accountType = "standard";
    }

    public BankAccount(String a, String p, int b, String type) {
        accNumber   = a;
        accPasswd   = p;
        balance     = b;
        accountType = type;
    }

    // Withdraw – overridden by subclasses for custom rules
    public boolean withdraw(int amount) {
        if (amount < 0 || balance < amount) return false;
        balance -= amount;
        return true;
    }

    // Deposit – overridden by subclasses for custom rules
    public boolean deposit(int amount) {
        if (amount < 0) return false;

        // --- NEW BALANCE LIMIT CHECK ---
        // Use long for the sum to prevent integer overflow if balance + amount exceeds Integer.MAX_VALUE
        if ((long)balance + amount > MAX_BALANCE) {
            System.out.println("Deposit failed: Exceeds maximum balance of £" + MAX_BALANCE);
            return false; // Deposit fails if it would exceed the max balance
        }
        // --- END NEW CHECK ---

        balance += amount;
        return true;
    }

    public int    getBalance()     { return balance; }
    public String getAccNumber()   { return accNumber; }
    public String getaccPasswd()   { return accPasswd; }
    public String getAccountType() { return accountType; }
}