package com.atmbanksimulator;

// ===== StudentAccount =====
// Has a daily withdrawal cap of £500.
// Once the cap is reached, no more withdrawals until next session.
public class StudentAccount extends BankAccount {

    private static final int DAILY_CAP = 500;
    private int dailyWithdrawn = 0;

    public StudentAccount(String a, String p, int b) {
        super(a, p, b, "student");
    }

    @Override
    public boolean withdraw(int amount) {
        if (amount <= 0) return false;
        if (dailyWithdrawn + amount > DAILY_CAP) return false; // daily cap exceeded
        if (balance < amount) return false;
        balance       -= amount;
        dailyWithdrawn += amount;
        return true;
    }

    // How much the student can still withdraw today
    public int getRemainingDailyLimit() {
        return DAILY_CAP - dailyWithdrawn;
    }
}