package com.atmbanksimulator;

// ===== PrimeAccount =====
// Supports overdraft up to £500.
// Balance can go negative but not below -500.
public class PrimeAccount extends BankAccount {

    private static final int OVERDRAFT_LIMIT = 500;

    public PrimeAccount(String a, String p, int b) {
        super(a, p, b, "prime");
    }

    @Override
    public boolean withdraw(int amount) {
        if (amount <= 0) return false;
        if (balance - amount < -OVERDRAFT_LIMIT) return false; // would exceed overdraft
        balance -= amount;
        return true;
    }

    public int getOverdraftLimit() {
        return OVERDRAFT_LIMIT;
    }
}