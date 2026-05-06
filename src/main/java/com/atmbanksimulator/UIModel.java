package com.atmbanksimulator;

import javafx.application.Platform;
import java.util.List;

public class UIModel {
    View view;
    private final Bank bank;

    // States
    private final String STATE_WELCOME        = "welcome";
    private final String STATE_ACCOUNT_NO     = "account_no";
    private final String STATE_PASSWORD       = "password";
    private final String STATE_LOGGED_IN      = "logged_in";
    private final String STATE_DEPOSIT_AMT    = "deposit_amt";
    private final String STATE_WITHDRAW_AMT   = "withdraw_amt";
    private final String STATE_WITHDRAW_QUICK = "withdraw_quick";
    private final String STATE_CHANGE_PW_OLD  = "change_pw_old";
    private final String STATE_CHANGE_PW_NEW  = "change_pw_new";
    private final String STATE_NEW_ACC_NO     = "new_acc_no";
    private final String STATE_NEW_ACC_PW     = "new_acc_pw";
    private final String STATE_NEW_ACC_TYPE   = "new_acc_type";
    private final String STATE_TRANSFER_ACC   = "transfer_acc";
    private final String STATE_TRANSFER_AMT   = "transfer_amt";

    private String state = STATE_WELCOME;
    private String accNumber = "";
    private String accPasswd = "";
    private String newAccNumber = "";
    private String newAccPasswd = "";
    private String transferDestAcc = "";

    private String message = "";
    private String numberPadInput = "";
    private String result = "";

    public UIModel(Bank bank) {
        this.bank = bank;
    }

    public void initialise() {
        // The View now owns the splash/welcome screen.
    }

    // =====================================================================
    // LOGIN
    // =====================================================================
    public void startManualLoginFlow() {
        resetTypedInput();
        accNumber = "";
        accPasswd = "";
        setState(STATE_ACCOUNT_NO);
        message = "Enter your account number";
        result = "Use the keypad below, then press Ent / Continue.";
        view.showManualLoginAccNumber();
        view.update(message, "", result);
    }

    public void processNFCLogin(String uid) {
        Platform.runLater(() -> {
            System.out.println("[UIModel] NFC login — UID: " + uid);
            if (bank.loginByUID(uid)) {
                SoundPlayer.playSuccess();
                String type = bank.getAccountType();
                view.showWelcomeGreeting(type, () -> {
                    setState(STATE_LOGGED_IN);
                    resetTypedInput();
                    message = "Welcome";
                    result = "Please select an option";
                    view.showATMPanel();
                    view.update(message, "", result);
                });
            } else {
                SoundPlayer.playError();
                setState(STATE_WELCOME);
                view.showResultPage(
                        "Card Not Recognised",
                        "This NFC card is not linked to an account.",
                        "Please try again, use Manual Login, or contact the bank to register your card."
                );
            }
        });
    }

    // =====================================================================
    // NUMBER PAD
    // =====================================================================
    public void processNumber(String n) {
        if (state.equals(STATE_LOGGED_IN) || state.equals(STATE_WELCOME)) return;
        if (numberPadInput.length() >= 14) return;
        numberPadInput += n;
        view.update(message, displayInput(), result);
    }

    public void processClear() {
        numberPadInput = "";
        view.update(message, "", result);
    }

    public void processEnter() {
        switch (state) {
            case STATE_ACCOUNT_NO -> confirmLoginAccountNumber();
            case STATE_PASSWORD -> confirmLoginPassword();
            case STATE_DEPOSIT_AMT -> confirmDeposit();
            case STATE_WITHDRAW_AMT -> confirmWithdraw();
            case STATE_TRANSFER_ACC -> confirmTransferDestination();
            case STATE_TRANSFER_AMT -> confirmTransferAmount();
            case STATE_CHANGE_PW_OLD -> confirmOldPin();
            case STATE_CHANGE_PW_NEW -> confirmNewPin();
            case STATE_NEW_ACC_NO -> confirmNewAccountNumber();
            case STATE_NEW_ACC_PW -> confirmNewAccountPin();
            case STATE_NEW_ACC_TYPE -> confirmNewAccountType();
            default -> { /* no action */ }
        }
    }

    private void confirmLoginAccountNumber() {
        if (numberPadInput.isEmpty()) {
            showInputError("Please enter your account number.");
            return;
        }
        accNumber = numberPadInput;
        numberPadInput = "";
        setState(STATE_PASSWORD);
        message = "Enter your PIN / password";
        result = "For your security, the input is hidden while you type.";
        view.showManualLoginPassword();
        view.update(message, "", result);
    }

    private void confirmLoginPassword() {
        if (numberPadInput.isEmpty()) {
            showInputError("Please enter your PIN / password.");
            return;
        }
        accPasswd = numberPadInput;
        numberPadInput = "";
        if (bank.login(accNumber, accPasswd)) {
            SoundPlayer.playSuccess();
            String type = bank.getAccountType();
            view.showWelcomeGreeting(type, () -> {
                setState(STATE_LOGGED_IN);
                message = "Welcome";
                result = "Please select an option";
                view.showATMPanel();
                view.update(message, "", result);
            });
        } else if (bank.isLocked(accNumber)) {
            SoundPlayer.playError();
            setState(STATE_WELCOME);
            view.showResultPage(
                    "Account Locked",
                    "Too many incorrect attempts.",
                    "This account has been locked. Please contact staff for help."
            );
        } else {
            SoundPlayer.playError();
            setState(STATE_PASSWORD);
            message = "Login failed — incorrect details";
            result = "Check the account number and PIN/password, then try again.";
            view.update(message, "", result);
        }
    }

    // =====================================================================
    // MAIN ACTIONS
    // =====================================================================
    public void processDeposit() {
        if (!requireLogin()) return;
        startTransaction(
                STATE_DEPOSIT_AMT,
                "Deposit",
                "Enter the amount you want to deposit",
                "Use the keypad below, then press Ent / Continue.",
                "Amount (£)"
        );
    }

    public void processWithdraw() {
        if (!requireLogin()) return;
        resetTypedInput();
        setState(STATE_WITHDRAW_QUICK);
        view.showWithdrawQuickOptions();
    }

    private void confirmDeposit() {
        int amount = parseAmount(numberPadInput);
        if (amount <= 0) {
            showInputError("Enter a valid deposit amount greater than £0.");
            return;
        }
        numberPadInput = "";
        if (bank.deposit(amount)) {
            SoundPlayer.playSuccess();
            setState(STATE_LOGGED_IN);
            view.showResultPage(
                    "Deposit Successful",
                    "Your cash has been added to the account.",
                    "Deposited: £" + amount + "\nNew balance: £" + bank.getBalance()
            );
        } else {
            SoundPlayer.playError();
            setState(STATE_LOGGED_IN);
            view.showResultPage(
                    "Deposit Failed",
                    "We could not complete the deposit.",
                    "Please check the amount and try again."
            );
        }
    }

    private void confirmWithdraw() {
        int amount = parseAmount(numberPadInput);
        if (amount <= 0) {
            showInputError("Enter a valid withdrawal amount greater than £0.");
            return;
        }
        numberPadInput = "";
        if (bank.withdraw(amount)) {
            SoundPlayer.playSuccess();
            setState(STATE_LOGGED_IN);
            view.showResultPage(
                    "Withdrawal Successful",
                    "Please collect your cash.",
                    "Withdrawn: £" + amount + "\nNew balance: £" + bank.getBalance() +
                            (bank.isLowBalance() ? "\n\nLow balance warning: your balance is below £50." : "")
            );
        } else {
            SoundPlayer.playError();
            setState(STATE_LOGGED_IN);
            view.showResultPage(
                    "Withdrawal Failed",
                    "There are insufficient funds or the amount is invalid.",
                    "Current balance: £" + bank.getBalance() + "\nPlease go back and try a smaller amount."
            );
        }
    }

    public void processBalance() {
        if (!requireLogin()) return;
        cancelFlowToLoggedIn();
        SoundPlayer.playSuccess();
        view.showResultPage(
                "Balance",
                "Your available balance is shown below.",
                "Available balance: £" + bank.getBalance() +
                        (bank.isLowBalance() ? "\n\nLow balance warning: your balance is below £50." : "")
        );
    }

    public void processMiniStatement() {
        if (!requireLogin()) return;
        cancelFlowToLoggedIn();
        List<String> txns = bank.getMiniStatement();
        StringBuilder sb = new StringBuilder();
        if (txns.isEmpty()) {
            sb.append("No transactions yet.");
        } else {
            for (int i = 0; i < txns.size(); i++) {
                sb.append(i + 1).append(". ").append(txns.get(i)).append("\n");
            }
        }
        SoundPlayer.playSuccess();
        view.showResultPage("Statement", "Last 5 transactions", sb.toString().trim());
    }

    public void processTransfer() {
        if (!requireLogin()) return;
        resetTypedInput();
        transferDestAcc = "";
        setState(STATE_TRANSFER_ACC);
        message = "Enter destination account number";
        result = "Type the recipient account number, then press Ent / Continue.";
        view.showInputPage("Transfer", message, result, "Destination account");
        view.update(message, "", result);
    }

    private void confirmTransferDestination() {
        if (numberPadInput.isEmpty()) {
            showInputError("Please enter the destination account number.");
            return;
        }
        transferDestAcc = numberPadInput;
        numberPadInput = "";
        setState(STATE_TRANSFER_AMT);
        message = "Enter transfer amount";
        result = "Sending to account " + transferDestAcc + ". Press Ent / Continue when ready.";
        view.showInputPage("Transfer", message, result, "Amount (£)");
        view.update(message, "", result);
    }

    private void confirmTransferAmount() {
        int amount = parseAmount(numberPadInput);
        if (amount <= 0) {
            showInputError("Enter a valid transfer amount greater than £0.");
            return;
        }
        numberPadInput = "";
        if (bank.transfer(transferDestAcc, amount)) {
            SoundPlayer.playSuccess();
            setState(STATE_LOGGED_IN);
            view.showResultPage(
                    "Transfer Successful",
                    "The money has been sent.",
                    "Sent: £" + amount + "\nTo account: " + transferDestAcc + "\nNew balance: £" + bank.getBalance()
            );
        } else {
            SoundPlayer.playError();
            setState(STATE_LOGGED_IN);
            view.showResultPage(
                    "Transfer Failed",
                    "We could not complete the transfer.",
                    "Check the recipient account and your available balance, then try again."
            );
        }
    }

    public void processChangePassword() {
        if (!requireLogin()) return;
        resetTypedInput();
        setState(STATE_CHANGE_PW_OLD);
        message = "Enter your current PIN / password";
        result = "Use the keypad, then press Ent / Continue.";
        view.showInputPage("Change PIN", message, result, "Current PIN");
        view.update(message, "", result);
    }

    private void confirmOldPin() {
        if (!numberPadInput.equals(bank.getLoggedInPassword())) {
            SoundPlayer.playError();
            numberPadInput = "";
            showInputError("Incorrect current PIN / password. Please try again.");
            return;
        }
        SoundPlayer.playSuccess();
        numberPadInput = "";
        setState(STATE_CHANGE_PW_NEW);
        message = "Enter a new PIN";
        result = "Use at least 4 digits, then press Ent / Continue.";
        view.showInputPage("Change PIN", message, result, "New PIN");
        view.update(message, "", result);
    }

    private void confirmNewPin() {
        String newPin = numberPadInput;
        if (newPin.length() < 4) {
            showInputError("Your new PIN must be at least 4 digits.");
            return;
        }
        numberPadInput = "";
        if (bank.changePassword(newPin)) {
            SoundPlayer.playSuccess();
            setState(STATE_LOGGED_IN);
            view.showResultPage(
                    "PIN Changed",
                    "Your PIN has been updated successfully.",
                    "Use your new PIN the next time you log in."
            );
        } else {
            SoundPlayer.playError();
            showInputError("Could not update PIN. Please try a different PIN.");
        }
    }

    // =====================================================================
    // CREATE ACCOUNT
    // =====================================================================
    public void processNewAccount() {
        resetTypedInput();
        newAccNumber = "";
        newAccPasswd = "";
        setState(STATE_NEW_ACC_NO);
        message = "Choose a new account number";
        result = "Enter at least 4 digits. You will use this number to log in.";
        view.showCreateAccountStep(1, message, "Account number", result, "New account number");
        view.update(message, "", result);
    }

    private void confirmNewAccountNumber() {
        if (numberPadInput.length() < 4) {
            showInputError("Account number must be at least 4 digits.");
            return;
        }
        newAccNumber = numberPadInput;
        numberPadInput = "";
        setState(STATE_NEW_ACC_PW);
        message = "Create a secure PIN";
        result = "Enter at least 4 digits. Keep it private and easy for you to remember.";
        view.showCreateAccountStep(2, message, "Secure PIN", result, "New PIN");
        view.update(message, "", result);
    }

    private void confirmNewAccountPin() {
        if (numberPadInput.length() < 4) {
            showInputError("PIN must be at least 4 digits.");
            return;
        }
        newAccPasswd = numberPadInput;
        numberPadInput = "";
        setState(STATE_NEW_ACC_TYPE);
        message = "Choose account type";
        result = "Press 1 Student, 2 Prime, 3 Saving, or 4 Standard. Then press Ent / Continue.";
        view.showCreateAccountStep(3, message, "Account type", result, "1 / 2 / 3 / 4");
        view.update(message, "", result);
    }

    private void confirmNewAccountType() {
        String typeChoice = numberPadInput;
        if (typeChoice.isEmpty()) {
            showInputError("Choose an account type: 1, 2, 3, or 4.");
            return;
        }
        String accType;
        switch (typeChoice) {
            case "1" -> accType = "student";
            case "2" -> accType = "prime";
            case "3" -> accType = "saving";
            case "4" -> accType = "standard";
            default -> {
                showInputError("Invalid choice. Use 1, 2, 3, or 4.");
                return;
            }
        }
        numberPadInput = "";
        if (bank.addBankAccount(newAccNumber, newAccPasswd, 0, accType)) {
            SoundPlayer.playSuccess();
            setState(STATE_WELCOME);
            view.showResultPage(
                    "Account Created",
                    "Your new " + accType.toUpperCase() + " account is ready.",
                    "Account number: " + newAccNumber + "\nYou can now go back and log in."
            );
            newAccNumber = "";
            newAccPasswd = "";
        } else {
            SoundPlayer.playError();
            setState(STATE_WELCOME);
            view.showResultPage(
                    "Account Not Created",
                    "This account number may already exist.",
                    "Please go back and try a different account number."
            );
            newAccNumber = "";
            newAccPasswd = "";
        }
    }

    // =====================================================================
    // LOGOUT / NAVIGATION / MUTE
    // =====================================================================
    public void processFinish() {
        if (!requireLogin()) return;
        bank.logout();
        SoundPlayer.playSuccess();
        setState(STATE_WELCOME);
        resetTypedInput();
        view.resetToWelcome(() -> startManualLoginFlow());
    }

    public void processGoBack() {
        SoundPlayer.playButtonPress();
        resetTypedInput();
        transferDestAcc = "";
        newAccNumber = "";
        newAccPasswd = "";
        // If on the numpad entered from "Other Amount", go back to quick options
        if (state.equals(STATE_WITHDRAW_AMT)) {
            processWithdraw();
            return;
        }
        if (bank.loggedIn()) {
            setState(STATE_LOGGED_IN);
            view.showATMPanel();
            view.update("Welcome", "", "Please select an option");
        } else {
            setState(STATE_WELCOME);
            view.resetToWelcome(() -> startManualLoginFlow());
        }
    }

    public void processMuteToggle() {
        boolean muted = SoundPlayer.toggleMute();
        if (!muted) SoundPlayer.playSuccess();
        view.setSoundMuted(SoundPlayer.isMuted());
    }

    public void processUnknownKey(String action) {
        SoundPlayer.playError();
        message = "Unknown command";
        result = "This action is not available here.";
        view.update(message, displayInput(), result);
    }

    // Legacy methods kept so old buttons / future calls do not break.
    public void processQuickWithdraw(int amount) {
        if (!requireLogin()) return;
        numberPadInput = String.valueOf(amount);
        setState(STATE_WITHDRAW_AMT);
        confirmWithdraw();
    }

    public void processWithdrawOther() {
        if (!requireLogin()) return;
        startTransaction(
                STATE_WITHDRAW_AMT,
                "Withdraw",
                "Enter the amount you want to withdraw",
                "Use the keypad below, then press Ent / Continue.",
                "Amount (\u00a3)"
        );
    }

    public void processWithdrawPrompt() {
        processWithdraw();
    }

    public void processDepositPrompt() {
        processDeposit();
    }

    // =====================================================================
    // HELPERS
    // =====================================================================
    private void startTransaction(String nextState, String title, String instruction, String helper, String displayPrompt) {
        resetTypedInput();
        setState(nextState);
        message = instruction;
        result = helper;
        view.showInputPage(title, instruction, helper, displayPrompt);
        view.update(instruction, "", helper);
    }

    private void showInputError(String text) {
        SoundPlayer.playError();
        message = text;
        view.update(text, displayInput(), result);
    }

    private void cancelFlowToLoggedIn() {
        resetTypedInput();
        setState(STATE_LOGGED_IN);
    }

    private void resetTypedInput() {
        numberPadInput = "";
    }

    private void setState(String s) {
        if (!state.equals(s)) {
            System.out.println("UIModel state: " + state + " → " + s);
            state = s;
        }
    }

    private boolean requireLogin() {
        if (!bank.loggedIn()) {
            SoundPlayer.playError();
            setState(STATE_WELCOME);
            view.showResultPage(
                    "Please Log In First",
                    "This option is only available after login.",
                    "Go back to the welcome screen and log in with your NFC card or account number."
            );
            return false;
        }
        return true;
    }

    private int parseAmount(String s) {
        if (s == null || s.isEmpty()) return 0;
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String displayInput() {
        if (state.equals(STATE_PASSWORD) || state.equals(STATE_CHANGE_PW_OLD) ||
                state.equals(STATE_CHANGE_PW_NEW) || state.equals(STATE_NEW_ACC_PW)) {
            return "•".repeat(numberPadInput.length());
        }
        return numberPadInput;
    }
}