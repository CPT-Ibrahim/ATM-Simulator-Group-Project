package com.atmbanksimulator;

// ===== SavingAccount =====
// Earns 3% interest on every deposit automatically.
// e.g. deposit £100 → account receives £103.
public class SavingAccount extends BankAccount {

    private static final double INTEREST_RATE = 0.03; // 3%

    public SavingAccount(String a, String p, int b) {
        super(a, p, b, "saving");
    }

    @Override
    public boolean deposit(int amount) {
        if (amount < 0) return false;
        int interest = (int)(amount * INTEREST_RATE);
        balance += amount + interest; // deposit + interest
        return true;
    }

    public double getInterestRate() {
        return INTEREST_RATE;
    }
}