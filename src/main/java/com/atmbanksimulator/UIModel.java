package com.atmbanksimulator;

public class UIModel {
    View view;
    private Bank bank;

    private final String STATE_ACCOUNT_NO    = "account_no";
    private final String STATE_PASSWORD      = "password";
    private final String STATE_LOGGED_IN     = "logged_in";
    private final String STATE_CHANGE_PW_OLD = "change_pw_old";
    private final String STATE_CHANGE_PW_NEW = "change_pw_new";
    private final String STATE_NEW_ACC_NO    = "new_acc_no";
    private final String STATE_NEW_ACC_PW    = "new_acc_pw";
    private final String STATE_NEW_ACC_TYPE  = "new_acc_type";

    // New States for Transfer
    private final String STATE_TRANSFER_ACC  = "transfer_acc";
    private final String STATE_TRANSFER_AMT  = "transfer_amt";

    private String state        = STATE_ACCOUNT_NO;
    private String accNumber    = "";
    private String accPasswd    = "";
    private String newAccNumber = "";
    private String newAccPasswd = "";

    // Variable to hold destination for transfer
    private String transferDestAcc = "";

    private String message;
    private String numberPadInput;
    private String result;

    public UIModel(Bank bank) {
        this.bank = bank;
    }

    // -----------------------------------------------------------------------
    // Main menu helper
    // -----------------------------------------------------------------------
    private String mainMenu() {
        return "========================\n" +
                "  MAIN MENU\n"             +
                "========================\n" +
                "  Dep = Deposit\n"         +
                "  W/D = Withdraw\n"        +
                "  Tra = Transfer\n"        + // Added Tra to menu
                "  Bal = Check Balance\n"   +
                "  ChP = Change Password\n" +
                "  New = Create Account\n"  +
                "  Fin = Logout\n"          +
                "========================";
    }

    // -----------------------------------------------------------------------
    // Welcome Page
    // -----------------------------------------------------------------------
    public void initialise() {
        setState(STATE_ACCOUNT_NO);
        numberPadInput = "";
        message = "Welcome to Brighton ATM";
        result  = "========================\n"       +
                "  Please enter your\n"            +
                "  account number\n"               +
                "  then press \"Ent\"\n"           +
                "========================\n"       +
                "  New user? Press \"New\"\n"      +
                "  to create an account\n"         +
                "========================\n"       +
                "  Press ? FAQ for help";
        update();
    }

    // -----------------------------------------------------------------------
    // Reset – Goodbye Page on logout, welcome otherwise
    // -----------------------------------------------------------------------
    private void reset(String msg) {
        setState(STATE_ACCOUNT_NO);
        numberPadInput = "";
        message = msg;
        if (msg.equals("Thank you for using the Bank ATM")) {
            result = "========================\n"  +
                    "  Goodbye!\n"               +
                    "  Have a great day.\n"      +
                    "========================\n"  +
                    "  Enter account number\n"   +
                    "  to start a new session\n" +
                    "  then press \"Ent\"";
        } else {
            result = "========================\n"  +
                    "  Enter your account\n"     +
                    "  number using keypad\n"    +
                    "  then press \"Ent\"\n"     +
                    "========================\n"  +
                    "  New user? Press \"New\"\n" +
                    "  Press ? FAQ for help";
        }
    }

    private void setState(String newState) {
        if (!state.equals(newState)) {
            System.out.println("UIModel::setState: " + state + " -> " + newState);
            state = newState;
        }
    }

    // -----------------------------------------------------------------------
    // Number / Clear
    // -----------------------------------------------------------------------
    public void processNumber(String numberOnButton) {
        numberPadInput += numberOnButton;
        message = "Beep! " + numberOnButton + " received";
        update();
    }

    public void processClear() {
        if (!numberPadInput.isEmpty()) {
            numberPadInput = "";
            message = "Input Cleared";
            update();
        }
    }

    // -----------------------------------------------------------------------
    // Enter – handles all states
    // -----------------------------------------------------------------------
    public void processEnter() {
        switch (state) {

            case STATE_ACCOUNT_NO:
                if (numberPadInput.equals("")) {
                    message = "Invalid Account Number";
                    reset(message);
                } else {
                    accNumber = numberPadInput;
                    numberPadInput = "";
                    setState(STATE_PASSWORD);
                    message = "Account Number Accepted";
                    result  = "========================\n" +
                            "  Now enter your\n"        +
                            "  password using keypad\n" +
                            "  then press \"Ent\"\n"    +
                            "========================\n" +
                            "  Press ? FAQ for help";
                }
                break;

            case STATE_PASSWORD:
                accPasswd = numberPadInput;
                numberPadInput = "";
                if (bank.login(accNumber, accPasswd)) {
                    setState(STATE_LOGGED_IN);
                    message = "Logged In Successfully";
                    result  = mainMenu();
                } else {
                    // Check if failure was due to account lock
                    if (bank.isLocked(accNumber)) {
                        message = "ACCOUNT LOCKED";
                        result = "========================\n" +
                                "  TOO MANY ATTEMPTS!\n"    +
                                "  This account is locked\n" +
                                "  until the app restarts.\n" +
                                "========================\n" +
                                "  Please contact staff.";
                        setState(STATE_ACCOUNT_NO);
                    } else {
                        message = "Login Failed: Incorrect PIN";
                        reset(message);
                    }
                }
                break;

            case STATE_TRANSFER_ACC:
                if (numberPadInput.isEmpty()) {
                    message = "Enter Target Account";
                } else {
                    transferDestAcc = numberPadInput;
                    numberPadInput = "";
                    setState(STATE_TRANSFER_AMT);
                    message = "Transfer to: " + transferDestAcc;
                    result = "========================\n" +
                            "  Transfer to: " + transferDestAcc + "\n" +
                            "  Enter AMOUNT to send\n" +
                            "  then press \"Ent\"\n" +
                            "========================";
                }
                break;

            case STATE_TRANSFER_AMT:
                int tAmount = parseValidAmount(numberPadInput);
                numberPadInput = "";
                if (bank.transfer(transferDestAcc, tAmount)) {
                    setState(STATE_LOGGED_IN);
                    message = "Transfer Successful";
                    result = "========================\n" +
                            "  Sent: £" + tAmount + "\n" +
                            "  To: " + transferDestAcc + "\n" +
                            "  New Balance: £" + bank.getBalance() + "\n" +
                            "========================\n" +
                            "  Bal / Dep / W/D / Fin";
                } else {
                    setState(STATE_LOGGED_IN);
                    message = "Transfer Failed";
                    result = "========================\n" +
                            "  TRANSACTION FAILED\n"   +
                            "  - Check recipient ID\n" +
                            "  - Check your balance\n" +
                            "========================\n" +
                            mainMenu();
                }
                break;

            case STATE_CHANGE_PW_OLD:
                if (numberPadInput.equals("") || !numberPadInput.equals(bank.getLoggedInPassword())) {
                    numberPadInput = "";
                    message = "Incorrect Old Password";
                    setState(STATE_LOGGED_IN);
                    result  = mainMenu();
                } else {
                    numberPadInput = "";
                    setState(STATE_CHANGE_PW_NEW);
                    message = "Old Password Verified";
                    result  = "========================\n"     +
                            "  Enter NEW password\n"         +
                            "  (min 6 chars,\n"             +
                            "  letters + digits)\n"         +
                            "  then press \"Ent\"\n"        +
                            "========================";
                }
                break;

            case STATE_CHANGE_PW_NEW:
                String newPw = numberPadInput;
                numberPadInput = "";
                if (bank.changePassword(newPw)) {
                    setState(STATE_LOGGED_IN);
                    message = "Password Changed Successfully";
                    result  = mainMenu();
                } else {
                    message = "Invalid Password";
                    result  = "========================\n"  +
                            "  Enter NEW password\n"     +
                            "  again\n"                  +
                            "  (min 6 chars,\n"          +
                            "  letters + digits)\n"      +
                            "  then press \"Ent\"\n"     +
                            "========================";
                }
                break;

            case STATE_NEW_ACC_NO:
                if (numberPadInput.equals("") || numberPadInput.length() < 4) {
                    message = "Account Number Too Short";
                    result  = "========================\n" +
                            "  Re-enter account\n"      +
                            "  number (min 4 digits)\n" +
                            "  then press \"Ent\"\n"    +
                            "========================";
                } else {
                    newAccNumber = numberPadInput;
                    numberPadInput = "";
                    setState(STATE_NEW_ACC_PW);
                    message = "Account Number Accepted";
                    result  = "========================\n"  +
                            "  Enter a password\n"       +
                            "  (min 6 chars,\n"          +
                            "  letters + digits)\n"      +
                            "  then press \"Ent\"\n"     +
                            "========================";
                }
                break;

            case STATE_NEW_ACC_PW:
                if (numberPadInput.length() < 6) {
                    message = "Password Too Short (min 6)";
                    result  = "========================\n"  +
                            "  Re-enter password\n"      +
                            "  (min 6 chars,\n"          +
                            "  letters + digits)\n"      +
                            "  then press \"Ent\"\n"     +
                            "========================";
                } else {
                    newAccPasswd = numberPadInput;
                    numberPadInput = "";
                    setState(STATE_NEW_ACC_TYPE);
                    message = "Password Accepted";
                    result  = "========================\n" +
                            "  Choose account type:\n"  +
                            "  1 = Student\n"           +
                            "  2 = Prime\n"             +
                            "  3 = Saving\n"            +
                            "  4 = Standard\n"          +
                            "  then press \"Ent\"\n"    +
                            "========================";
                }
                break;

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
                    newAccNumber = "";
                    newAccPasswd = "";
                    reset("Account Created Successfully");
                } else {
                    newAccNumber = "";
                    newAccPasswd = "";
                    reset("Account Already Exists or Creation Failed");
                }
                break;

            case STATE_LOGGED_IN:
            default:
                // no-op
        }
        update();
    }

    // -----------------------------------------------------------------------
    // Transfer
    // -----------------------------------------------------------------------
    public void processTransfer() {
        if (state.equals(STATE_LOGGED_IN)) {
            numberPadInput = "";
            transferDestAcc = "";
            setState(STATE_TRANSFER_ACC);
            message = "Transfer Funds";
            result  = "========================\n" +
                    "  Enter DESTINATION\n"      +
                    "  account number\n"         +
                    "  then press \"Ent\"\n"     +
                    "========================\n" +
                    "  Press CLR to cancel";
        } else {
            reset("You Are Not Logged In");
        }
        update();
    }

    // -----------------------------------------------------------------------
    // Balance
    // -----------------------------------------------------------------------
    public void processBalance() {
        if (state.equals(STATE_LOGGED_IN)) {
            numberPadInput = "";
            message = "Balance Available";
            result  = "========================\n"              +
                    "  Your balance is:\n"                   +
                    "  \u00A3" + bank.getBalance() + "\n"   +
                    "========================\n"              +
                    "  Dep / W/D / ChP / Fin";
        } else {
            reset("You Are Not Logged In");
        }
        update();
    }

    // -----------------------------------------------------------------------
    // Withdraw
    // -----------------------------------------------------------------------
    public void processWithdraw() {
        if (state.equals(STATE_LOGGED_IN)) {
            int amount = parseValidAmount(numberPadInput);
            numberPadInput = "";
            if (amount > 0) {
                if (bank.withdraw(amount)) {
                    message = "Withdrawal Successful";
                    result  = "========================\n"                       +
                            "  Withdrawn:\n"                                  +
                            "  \u00A3" + amount + "\n"                        +
                            "  New balance:\n"                                +
                            "  \u00A3" + bank.getBalance() + "\n"            +
                            "========================\n"                       +
                            "  Dep / W/D / Bal / Fin";
                } else {
                    message = "Withdrawal Failed";
                    result  = "========================\n"                       +
                            "  Insufficient funds\n"                          +
                            "  Balance:\n"                                    +
                            "  \u00A3" + bank.getBalance() + "\n"            +
                            "========================\n"                       +
                            "  Please enter new amount";
                }
            } else {
                message = "Invalid Amount";
                result  = "========================\n"    +
                        "  Enter a valid amount\n"     +
                        "  then press W/D\n"           +
                        "========================";
            }
        } else {
            reset("You Are Not Logged In");
        }
        update();
    }

    // -----------------------------------------------------------------------
    // Deposit
    // -----------------------------------------------------------------------
    public void processDeposit() {
        if (state.equals(STATE_LOGGED_IN)) {
            int amount = parseValidAmount(numberPadInput);
            numberPadInput = "";
            if (amount > 0) {
                bank.deposit(amount);
                message = "Deposit Successful";
                result  = "========================\n"                       +
                        "  Deposited:\n"                                  +
                        "  \u00A3" + amount + "\n"                        +
                        "  New balance:\n"                                +
                        "  \u00A3" + bank.getBalance() + "\n"            +
                        "========================\n"                       +
                        "  Dep / W/D / Bal / Fin";
            } else {
                message = "Invalid Amount";
                result  = "========================\n"   +
                        "  Enter a valid amount\n"    +
                        "  then press Dep\n"          +
                        "========================";
            }
        } else {
            reset("You Are Not Logged In");
        }
        update();
    }

    // -----------------------------------------------------------------------
    // Change Password
    // -----------------------------------------------------------------------
    public void processChangePassword() {
        if (state.equals(STATE_LOGGED_IN)) {
            numberPadInput = "";
            setState(STATE_CHANGE_PW_OLD);
            message = "Change Password";
            result  = "========================\n" +
                    "  Enter your OLD\n"         +
                    "  password using keypad\n"  +
                    "  then press \"Ent\"\n"     +
                    "========================\n" +
                    "  Press CLR to clear";
        } else {
            reset("You Are Not Logged In");
        }
        update();
    }

    // -----------------------------------------------------------------------
    // New Account
    // -----------------------------------------------------------------------
    public void processNewAccount() {
        if (state.equals(STATE_ACCOUNT_NO)) {
            numberPadInput = "";
            newAccNumber = "";
            newAccPasswd = "";
            setState(STATE_NEW_ACC_NO);
            message = "Create New Account";
            result  = "========================\n"  +
                    "  Enter a new account\n"     +
                    "  number (min 4 digits)\n"   +
                    "  then press \"Ent\"\n"      +
                    "========================\n"  +
                    "  Press CLR to clear";
        } else {
            reset("Please Log Out First");
        }
        update();
    }

    // -----------------------------------------------------------------------
    // Finish / Logout
    // -----------------------------------------------------------------------
    public void processFinish() {
        if (state.equals(STATE_LOGGED_IN)) {
            bank.logout();
            reset("Thank you for using the Bank ATM");
        } else {
            reset("You Are Not Logged In");
        }
        update();
    }

    // -----------------------------------------------------------------------
    // Unknown key
    // -----------------------------------------------------------------------
    public void processUnknownKey(String action) {
        reset("Invalid Command");
        update();
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------
    private int parseValidAmount(String number) {
        if (number.isEmpty()) return 0;
        try {
            return Integer.parseInt(number);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void update() {
        view.update(message, numberPadInput, result);
    }
}