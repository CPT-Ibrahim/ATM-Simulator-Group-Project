package com.atmbanksimulator;

import javafx.application.Platform;
import java.util.List;

public class UIModel {
    View view;
    private Bank bank;

    // States
    private final String STATE_WELCOME       = "welcome";
    private final String STATE_ACCOUNT_NO    = "account_no";
    private final String STATE_PASSWORD      = "password";
    private final String STATE_LOGGED_IN     = "logged_in";
    private final String STATE_CHANGE_PW_OLD = "change_pw_old";
    private final String STATE_CHANGE_PW_NEW = "change_pw_new";
    private final String STATE_NEW_ACC_NO    = "new_acc_no";
    private final String STATE_NEW_ACC_PW    = "new_acc_pw";
    private final String STATE_NEW_ACC_TYPE  = "new_acc_type";
    private final String STATE_TRANSFER_ACC  = "transfer_acc";
    private final String STATE_TRANSFER_AMT  = "transfer_amt";

    private String state        = STATE_WELCOME;
    private String accNumber    = "";
    private String accPasswd    = "";
    private String newAccNumber = "";
    private String newAccPasswd = "";
    private String transferDestAcc = "";

    private String message        = "";
    private String numberPadInput = "";
    private String result         = "";

    // Remembers the onManualLogin lambda so logout can restore the welcome page
    private Runnable onManualLoginLambda;

    public UIModel(Bank bank) {
        this.bank = bank;
    }

    // -----------------------------------------------------------------------
    // Called by Main.java after the view is ready (NFC idle state)
    // -----------------------------------------------------------------------
    public void initialise() {
        // No-op in new architecture — view.initAndShowWelcome() handles the splash.
    }

    // -----------------------------------------------------------------------
    // Called by Main.java when "Login Manually" is pressed on the splash
    // -----------------------------------------------------------------------
    public void startManualLoginFlow() {
        setState(STATE_ACCOUNT_NO);
        numberPadInput = "";
        accNumber = "";
        accPasswd = "";
        message = "Enter Account Number";
        result  = "";
        // Show the numpad input prompt page
        view.showManualLoginAccNumber();
        view.setNumpadVisible(true);
        view.update(message, numberPadInput, result);
    }

    // -----------------------------------------------------------------------
    // processNFCLogin — called from NFCServer thread, dispatched to FX thread
    // -----------------------------------------------------------------------
    public void processNFCLogin(String uid) {
        Platform.runLater(() -> {
            System.out.println("[UIModel] NFC login — UID: " + uid);
            if (bank.loginByUID(uid)) {
                SoundPlayer.playSuccess();
                String type = bank.getAccountType();
                // Show animated greeting, then transition to main menu
                view.showWelcomeGreeting(type, () -> {
                    setState(STATE_LOGGED_IN);
                    message = "Welcome — " + type.toUpperCase() + " account";
                    result  = mainMenu();
                    view.showATMPanel();
                    view.update(message, "", result);
                    view.setNumpadVisible(false);
                    view.setQuickButtonsVisible(false);
                });
            } else {
                SoundPlayer.playError();
                message = "Card Not Recognised";
                result  = "========================\n" +
                        "  This card is not\n"      +
                        "  linked to any account\n" +
                        "========================\n" +
                        "  Please contact your\n"   +
                        "  bank to register\n"      +
                        "  your NFC card\n"         +
                        "========================\n" +
                        "  Tap again to retry";
                view.update(message, "", result);
            }
        });
    }

    // -----------------------------------------------------------------------
    // Main menu text
    // -----------------------------------------------------------------------
    private String mainMenu() {
        return "========================\n" +
                "  Ready — use the\n"       +
                "  buttons to continue.\n"  +
                "========================";
    }

    // -----------------------------------------------------------------------
    // Number / Clear
    // -----------------------------------------------------------------------
    public void processNumber(String n) {
        numberPadInput += n;
        message = "Input: " + mask(numberPadInput);
        view.update(message, numberPadInput, result);
    }

    public void processClear() {
        numberPadInput = "";
        message = "Input Cleared";
        view.update(message, numberPadInput, result);
    }

    // -----------------------------------------------------------------------
    // Enter
    // -----------------------------------------------------------------------
    public void processEnter() {
        switch (state) {

            // ── Manual login: step 1 – account number ───────────────────
            case STATE_ACCOUNT_NO:
                if (numberPadInput.isEmpty()) {
                    message = "Please enter account number";
                    SoundPlayer.playError();
                    view.update(message, numberPadInput, result);
                } else {
                    accNumber = numberPadInput;
                    numberPadInput = "";
                    setState(STATE_PASSWORD);
                    message = "Account: " + accNumber;
                    result  = "";
                    view.showManualLoginPassword();
                    view.setNumpadVisible(true);
                    view.update(message, numberPadInput, result);
                }
                break;

            // ── Manual login: step 2 – password ─────────────────────────
            case STATE_PASSWORD:
                accPasswd = numberPadInput;
                numberPadInput = "";
                if (bank.login(accNumber, accPasswd)) {
                    SoundPlayer.playSuccess();
                    String type = bank.getAccountType();
                    view.showWelcomeGreeting(type, () -> {
                        setState(STATE_LOGGED_IN);
                        message = "Welcome — " + type.toUpperCase() + " account";
                        result  = mainMenu();
                        view.showATMPanel();
                        view.update(message, "", result);
                        view.setNumpadVisible(false);
                        view.setQuickButtonsVisible(false);
                    });
                } else {
                    if (bank.isLocked(accNumber)) {
                        SoundPlayer.playError();
                        message = "ACCOUNT LOCKED";
                        result  = "========================\n" +
                                "  TOO MANY ATTEMPTS!\n"    +
                                "  This account is locked\n"+
                                "  Please contact staff.\n" +
                                "========================";
                        setState(STATE_WELCOME);
                        view.setNumpadVisible(false);
                        view.update(message, "", result);
                    } else {
                        SoundPlayer.playError();
                        message = "Login Failed — incorrect credentials";
                        view.update(message, "", result);
                    }
                }
                break;

            // ── Transfer: destination account ───────────────────────────
            case STATE_TRANSFER_ACC:
                if (numberPadInput.isEmpty()) {
                    message = "Enter destination account";
                    SoundPlayer.playError();
                    view.update(message, numberPadInput, result);
                } else {
                    transferDestAcc = numberPadInput;
                    numberPadInput  = "";
                    setState(STATE_TRANSFER_AMT);
                    message = "Transfer to: " + transferDestAcc;
                    result  = "========================\n" +
                            "  Transfer to:\n"          +
                            "  " + transferDestAcc + "\n" +
                            "========================\n" +
                            "  Enter AMOUNT then Ent";
                    SoundPlayer.playSuccess();
                    view.update(message, numberPadInput, result);
                }
                break;

            // ── Transfer: amount ─────────────────────────────────────────
            case STATE_TRANSFER_AMT:
                int tAmt = parseAmount(numberPadInput);
                numberPadInput = "";
                if (bank.transfer(transferDestAcc, tAmt)) {
                    setState(STATE_LOGGED_IN);
                    SoundPlayer.playSuccess();
                    message = "Transfer Successful";
                    result  = "========================\n" +
                            "  Sent:    £" + tAmt + "\n"           +
                            "  To:      " + transferDestAcc + "\n" +
                            "  Balance: £" + bank.getBalance() + "\n" +
                            "========================\n" +
                            mainMenu();
                    view.setNumpadVisible(false);
                } else {
                    setState(STATE_LOGGED_IN);
                    SoundPlayer.playError();
                    message = "Transfer Failed";
                    result  = "========================\n"  +
                            "  TRANSACTION FAILED\n"     +
                            "  Check recipient & balance\n" +
                            "========================\n"  +
                            mainMenu();
                    view.setNumpadVisible(false);
                }
                view.update(message, "", result);
                break;

            // ── Change password: verify old ──────────────────────────────
            case STATE_CHANGE_PW_OLD:
                if (!numberPadInput.equals(bank.getLoggedInPassword())) {
                    numberPadInput = "";
                    message = "Incorrect old password";
                    SoundPlayer.playError();
                    setState(STATE_LOGGED_IN);
                    result  = mainMenu();
                    view.setNumpadVisible(false);
                    view.update(message, "", result);
                } else {
                    numberPadInput = "";
                    setState(STATE_CHANGE_PW_NEW);
                    SoundPlayer.playSuccess();
                    message = "Old password verified — enter new";
                    result  = "========================\n"    +
                            "  Enter NEW password\n"       +
                            "  (min 6 chars, letters+digits)\n" +
                            "  then press Ent\n"           +
                            "========================";
                    view.update(message, numberPadInput, result);
                }
                break;

            // ── Change password: new password ────────────────────────────
            case STATE_CHANGE_PW_NEW:
                String newPw = numberPadInput;
                numberPadInput = "";
                if (bank.changePassword(newPw)) {
                    setState(STATE_LOGGED_IN);
                    SoundPlayer.playSuccess();
                    message = "Password Changed Successfully";
                    result  = mainMenu();
                    view.setNumpadVisible(false);
                } else {
                    message = "Invalid password — try again";
                    SoundPlayer.playError();
                    result  = "========================\n"  +
                            "  Password must be:\n"      +
                            "  min 6 chars, has letters\n"+
                            "  and digits\n"             +
                            "========================";
                }
                view.update(message, numberPadInput, result);
                break;

            // ── New account: number ──────────────────────────────────────
            case STATE_NEW_ACC_NO:
                if (numberPadInput.length() < 4) {
                    message = "Account number too short (min 4)";
                    SoundPlayer.playError();
                    view.update(message, numberPadInput, result);
                } else {
                    newAccNumber = numberPadInput;
                    numberPadInput = "";
                    setState(STATE_NEW_ACC_PW);
                    SoundPlayer.playSuccess();
                    message = "Number accepted — enter password";
                    result  = "========================\n"  +
                            "  Enter a password\n"       +
                            "  (min 6 chars,\n"          +
                            "  letters + digits)\n"      +
                            "  then press Ent\n"         +
                            "========================";
                    view.update(message, numberPadInput, result);
                }
                break;

            // ── New account: password ────────────────────────────────────
            case STATE_NEW_ACC_PW:
                if (numberPadInput.length() < 6) {
                    message = "Password too short (min 6)";
                    SoundPlayer.playError();
                    view.update(message, numberPadInput, result);
                } else {
                    newAccPasswd = numberPadInput;
                    numberPadInput = "";
                    setState(STATE_NEW_ACC_TYPE);
                    SoundPlayer.playSuccess();
                    message = "Password accepted — choose type";
                    result  = "========================\n" +
                            "  Choose account type:\n"  +
                            "  1 = Student\n"           +
                            "  2 = Prime\n"             +
                            "  3 = Saving\n"            +
                            "  4 = Standard\n"          +
                            "  then press Ent\n"        +
                            "========================";
                    view.update(message, numberPadInput, result);
                }
                break;

            // ── New account: type choice ─────────────────────────────────
            case STATE_NEW_ACC_TYPE:
                String typeChoice = numberPadInput;
                numberPadInput = "";
                String accType;
                switch (typeChoice) {
                    case "1": accType = "student"; break;
                    case "2": accType = "prime";   break;
                    case "3": accType = "saving";  break;
                    default:  accType = "standard";
                }
                if (bank.addBankAccount(newAccNumber, newAccPasswd, 0, accType)) {
                    newAccNumber = ""; newAccPasswd = "";
                    SoundPlayer.playSuccess();
                    message = "Account Created Successfully";
                    setState(STATE_WELCOME);
                    view.setNumpadVisible(false);
                    view.update(message, "", "========================\n" +
                            "  Account created!\n"       +
                            "  You can now log in.\n"    +
                            "========================");
                } else {
                    newAccNumber = ""; newAccPasswd = "";
                    SoundPlayer.playError();
                    message = "Account already exists or creation failed";
                    setState(STATE_WELCOME);
                    view.setNumpadVisible(false);
                    view.update(message, "", result);
                }
                break;

            default:
                // no-op for STATE_LOGGED_IN (actions handled by dedicated methods)
                break;
        }
    }

    // -----------------------------------------------------------------------
    // Transfer
    // -----------------------------------------------------------------------
    public void processTransfer() {
        if (!requireLogin()) return; cancelFlow();
        numberPadInput = ""; transferDestAcc = "";
        setState(STATE_TRANSFER_ACC);
        SoundPlayer.playButtonPress();
        message = "Transfer — Enter Destination Account";
        result  = "========================\n" +
                "  Enter DESTINATION\n"      +
                "  account number\n"         +
                "  then press Ent\n"         +
                "========================\n" +
                "  Press CLR to cancel";
        view.setNumpadVisible(true);
        view.update(message, numberPadInput, result);
    }

    // -----------------------------------------------------------------------
    // Balance
    // -----------------------------------------------------------------------
    public void processBalance() {
        if (!requireLogin()) return; cancelFlow();
        numberPadInput = "";
        SoundPlayer.playSuccess();
        String lowWarn = bank.isLowBalance() ? "\n  !! LOW BALANCE !!\n  Consider depositing." : "";
        message = "Balance Available";
        result  = "========================\n"            +
                "  Your balance is:\n"                 +
                "  £" + bank.getBalance() + "\n"       +
                "========================\n"            +
                "  Deposit / Withdraw / Logout"              +
                lowWarn;
        view.setNumpadVisible(false);
        view.update(message, "", result);
    }

    // -----------------------------------------------------------------------
    // Mini Statement
    // -----------------------------------------------------------------------
    public void processMiniStatement() {
        if (!requireLogin()) return; cancelFlow();
        numberPadInput = "";
        List<String> txns = bank.getMiniStatement();
        StringBuilder sb = new StringBuilder();
        sb.append("========================\n");
        sb.append("  MINI STATEMENT\n");
        sb.append("  Last 5 transactions\n");
        sb.append("========================\n");
        if (txns.isEmpty()) {
            sb.append("  No transactions yet.\n");
        } else {
            for (int i = 0; i < txns.size(); i++)
                sb.append("  ").append(i+1).append(". ").append(txns.get(i)).append("\n");
        }
        sb.append("========================");
        if (bank.isLowBalance()) sb.append("\n  !! LOW BALANCE !!");
        SoundPlayer.playSuccess();
        message = "Mini Statement";
        result  = sb.toString();
        view.setNumpadVisible(false);
        view.update(message, "", result);
    }

    // -----------------------------------------------------------------------
    // Quick Withdraw
    // -----------------------------------------------------------------------
    public void processQuickWithdraw(int amount) {
        if (!requireLogin()) return; cancelFlow();
        if (bank.withdraw(amount)) {
            SoundPlayer.playSuccess();
            message = "Quick Withdrawal — £" + amount;
            result  = "========================\n"                    +
                    "  Withdrawn:  £" + amount + "\n"              +
                    "  Balance:    £" + bank.getBalance() + "\n"   +
                    "========================\n"                    +
                    mainMenu()                                      +
                    (bank.isLowBalance() ? "\n  !! LOW BALANCE !!" : "");
        } else {
            SoundPlayer.playError();
            message = "Insufficient Funds";
            result  = "========================\n"                       +
                    "  Insufficient funds\n"                          +
                    "  Balance: £" + bank.getBalance() + "\n"         +
                    "========================\n"                       +
                    "  Please try a smaller amount";
        }
        view.setQuickButtonsVisible(false);
        view.setNumpadVisible(false);
        view.update(message, "", result);
    }

    // -----------------------------------------------------------------------
    // Withdraw — "Other Amount": shows numpad so user can type custom value
    // -----------------------------------------------------------------------
    public void processWithdrawOther() {
        if (!requireLogin()) return; cancelFlow();
        numberPadInput = "";
        message = "Enter withdrawal amount then press Withdraw";
        result  = "========================\n"  +
                "  Enter amount (£)\n"        +
                "  then press Withdraw\n"     +
                "========================";
        view.setQuickButtonsVisible(false);
        view.setNumpadVisible(true);
        view.update(message, numberPadInput, result);
    }

    // -----------------------------------------------------------------------
    // Withdraw
    // -----------------------------------------------------------------------
    public void processWithdraw() {
        if (!requireLogin()) return;
        // Capture input BEFORE cancelFlow() clears it (same logic as processDeposit).
        int amount = state.equals(STATE_LOGGED_IN) ? parseAmount(numberPadInput) : 0;
        cancelFlow();
        if (amount > 0) {
            // ── Execute the custom amount entered via numpad ──────────────
            if (bank.withdraw(amount)) {
                SoundPlayer.playSuccess();
                message = "Withdrawal Successful";
                result  = "========================\n"                    +
                        "  Withdrawn:  £" + amount + "\n"              +
                        "  Balance:    £" + bank.getBalance() + "\n"   +
                        "========================\n"                    +
                        mainMenu();
            } else {
                SoundPlayer.playError();
                message = "Withdrawal Failed — Insufficient Funds";
                result  = "========================\n"                    +
                        "  Insufficient funds\n"                       +
                        "  Balance: £" + bank.getBalance() + "\n"      +
                        "========================\n"                    +
                        "  Please enter new amount";
            }
            view.setNumpadVisible(false);
            view.setQuickButtonsVisible(false);
        } else {
            // ── No amount entered → show quick-withdraw options ───────────
            message = "Select withdrawal amount";
            result  = "========================\n"  +
                    "  Choose a quick amount\n"  +
                    "  or 'Other Amount' to\n"   +
                    "  enter a custom value.\n"  +
                    "========================";
            view.setNumpadVisible(false);
            view.setQuickButtonsVisible(true);
        }
        view.update(message, "", result);
    }

    // -----------------------------------------------------------------------
    // Deposit
    // -----------------------------------------------------------------------
    public void processDeposit() {
        if (!requireLogin()) return;
        // Capture input BEFORE cancelFlow() clears it.
        // Only treat it as an amount if we are in the idle logged-in state;
        // if mid-flow (e.g. mid-transfer) the typed digits belong to that
        // flow and should NOT be used as a deposit amount.
        int amount = state.equals(STATE_LOGGED_IN) ? parseAmount(numberPadInput) : 0;
        cancelFlow();
        if (amount > 0) {
            if (bank.deposit(amount)) {
                SoundPlayer.playSuccess();
                message = "Deposit Successful";
                result  = "========================\n"                    +
                        "  Deposited:  £" + amount + "\n"              +
                        "  Balance:    £" + bank.getBalance() + "\n"   +
                        "========================\n"                    +
                        mainMenu();
            } else {
                SoundPlayer.playError();
                message = "Deposit Failed";
                result  = "========================\n"     +
                        "  Deposit could not be made\n" +
                        "  Please try again\n"          +
                        "========================";
            }
            view.setNumpadVisible(false);
            view.update(message, "", result);
        } else {
            // No amount yet — show the numpad so the user can type one,
            // then press Deposit again to confirm.
            message = "Enter deposit amount then press Deposit";
            result  = "========================\n" +
                    "  Enter amount (£)\n"       +
                    "  then press Deposit\n"     +
                    "========================";
            view.setNumpadVisible(true);
            view.update(message, numberPadInput, result);
        }
    }

    // -----------------------------------------------------------------------
    // Change Password
    // -----------------------------------------------------------------------
    public void processChangePassword() {
        if (!requireLogin()) return; cancelFlow();
        numberPadInput = "";
        setState(STATE_CHANGE_PW_OLD);
        message = "Change Password";
        result  = "========================\n" +
                "  Enter your OLD\n"         +
                "  password then Ent\n"      +
                "========================\n" +
                "  Press CLR to clear";
        view.setNumpadVisible(true);
        view.update(message, numberPadInput, result);
    }

    // -----------------------------------------------------------------------
    // New Account
    // -----------------------------------------------------------------------
    public void processNewAccount() {
        // Can be called from welcome screen (STATE_WELCOME) or logged-in state
        numberPadInput = ""; newAccNumber = ""; newAccPasswd = "";
        setState(STATE_NEW_ACC_NO);
        message = "Create New Account";
        result  = "========================\n"  +
                "  Enter a new account\n"     +
                "  number (min 4 digits)\n"   +
                "  then press Ent\n"          +
                "========================\n"  +
                "  Press CLR to clear";
        view.setNumpadVisible(true);
        view.update(message, numberPadInput, result);
    }

    // -----------------------------------------------------------------------
    // Finish / Logout
    // -----------------------------------------------------------------------
    public void processFinish() {
        if (!requireLogin()) return; cancelFlow();
        bank.logout();
        SoundPlayer.playSuccess();
        setState(STATE_WELCOME);
        view.setNumpadVisible(false);
        view.setQuickButtonsVisible(false);
        // Return to welcome splash (same window, animated transition)
        view.resetToWelcome(() -> startManualLoginFlow());
    }

    // -----------------------------------------------------------------------
    // Mute toggle
    // -----------------------------------------------------------------------
    public void processMuteToggle() {
        boolean muted = SoundPlayer.toggleMute();
        message = muted ? "Sounds Muted" : "Sounds Enabled";
        if (!muted) SoundPlayer.playSuccess();
        view.setSoundMuted(SoundPlayer.isMuted());
        view.update(message, numberPadInput, result);
    }

    // -----------------------------------------------------------------------
    // Unknown key
    // -----------------------------------------------------------------------
    public void processUnknownKey(String action) {
        SoundPlayer.playError();
        message = "Unknown command: " + action;
        view.update(message, numberPadInput, result);
    }

    // -----------------------------------------------------------------------
    // Deposit (numpad-first: shows numpad prompt)
    // Withdraw (numpad-first: shows numpad prompt)
    // These are called by action buttons — they reveal the numpad and prompt.
    // -----------------------------------------------------------------------
    public void processWithdrawPrompt() {
        if (!requireLogin()) return; cancelFlow();
        numberPadInput = "";
        message = "Enter withdrawal amount then press W/D";
        result  = "========================\n"  +
                "  Enter amount (£)\n"        +
                "  then press W/D\n"          +
                "========================";
        view.setNumpadVisible(true);
        view.update(message, numberPadInput, result);
    }

    public void processDepositPrompt() {
        if (!requireLogin()) return; cancelFlow();
        numberPadInput = "";
        message = "Enter deposit amount then press Dep";
        result  = "========================\n"  +
                "  Enter amount (£)\n"        +
                "  then press Dep\n"          +
                "========================";
        view.setNumpadVisible(true);
        view.update(message, numberPadInput, result);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------
    private void setState(String s) {
        if (!state.equals(s)) {
            System.out.println("UIModel state: " + state + " → " + s);
            state = s;
        }
    }

    /**
     * Cancels any in-progress multi-step flow and resets to the base logged-in state.
     * Call this at the start of every action method so pressing a new button always
     * cleanly cancels whatever was in progress.
     */
    private void cancelFlow() {
        numberPadInput  = "";
        transferDestAcc = "";
        newAccNumber    = "";
        newAccPasswd    = "";
        setState(STATE_LOGGED_IN);
        view.setNumpadVisible(false);
        view.setQuickButtonsVisible(false);
    }

    /**
     * Guards action methods using bank.loggedIn() — NOT state equality.
     * Mid-flow states (STATE_TRANSFER_ACC, STATE_CHANGE_PW_OLD, etc.) are NOT
     * STATE_LOGGED_IN, so the old state check wrongly blocked valid actions.
     * Returns true if the user is logged in (action may proceed).
     */
    private boolean requireLogin() {
        if (!bank.loggedIn()) {
            SoundPlayer.playError();
            message = "Please log in first";
            view.update(message, "", result);
            return false;
        }
        return true;
    }

    private void notLoggedIn() {
        SoundPlayer.playError();
        message = "Please log in first";
        view.update(message, "", result);
    }

    private int parseAmount(String s) {
        if (s == null || s.isEmpty()) return 0;
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 0; }
    }

    /** Mask input for display (show * except for account-number step) */
    private String mask(String s) {
        if (state.equals(STATE_PASSWORD) || state.equals(STATE_CHANGE_PW_OLD) ||
                state.equals(STATE_CHANGE_PW_NEW) || state.equals(STATE_NEW_ACC_PW)) {
            return "*".repeat(s.length());
        }
        return s;
    }
}