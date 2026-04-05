package com.atmbanksimulator;

// ===== BankAccount (Base Class) =====
// Base class for all account types.
// Fields are protected so subclasses can access them directly.
public class BankAccount {

    protected String accNumber  = "";
    protected String accPasswd  = "";
    protected int    balance    = 0;
    protected String accountType = "standard";

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
        balance += amount;
        return true;
    }

    public int    getBalance()     { return balance; }
    public String getAccNumber()   { return accNumber; }
    public String getaccPasswd()   { return accPasswd; }
    public String getAccountType() { return accountType; }
}