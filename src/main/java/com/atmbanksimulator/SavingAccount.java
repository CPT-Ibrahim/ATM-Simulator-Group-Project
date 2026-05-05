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
        int totalCredit = amount + interest; // The total amount that will be added

        // --- NEW BALANCE LIMIT CHECK (including interest) ---
        // Use long for the sum to prevent integer overflow if balance + totalCredit exceeds Integer.MAX_VALUE
        if ((long)balance + totalCredit > MAX_BALANCE) {
            System.out.println("SavingAccount deposit failed: Exceeds maximum balance of £" + MAX_BALANCE + " (including interest)");
            return false; // Deposit fails if it would exceed the max balance
        }
        // --- END NEW CHECK ---

        balance += totalCredit; // deposit + interest
        return true;
    }

    public double getInterestRate() {
        return INTEREST_RATE;
    }
}