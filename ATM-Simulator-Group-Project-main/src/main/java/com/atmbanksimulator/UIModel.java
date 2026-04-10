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

    private String state        = STATE_ACCOUNT_NO;
    private String accNumber    = "";
    private String accPasswd    = "";
    private String newAccNumber = "";
    private String newAccPasswd = "";

    private String message;
    private String numberPadInput;
    private String result;

    public UIModel(Bank bank) {
        this.bank = bank;
    }

    // -----------------------------------------------------------------------
    // FAQ – appended to every screen
    // -----------------------------------------------------------------------
    private String faq() {
        return "\n================================\n" +
               "  FAQ\n"                             +
               "================================\n"  +
               "\n"                                  +
               "  Q: How do I withdraw cash?\n"      +
               "  A: Enter amount + press W/D\n"     +
               "\n"                                  +
               "  Q: How do I check my balance?\n"   +
               "  A: Press Bal when logged in\n"     +
               "\n"                                  +
               "  Q: How do I change my PIN?\n"      +
               "  A: Press ChP when logged in\n"     +
               "\n"                                  +
               "  Q: How do I deposit?\n"            +
               "  A: Enter amount + press Dep\n"     +
               "\n"                                  +
               "  Q: How many PIN attempts\n"        +
               "     are allowed?\n"                 +
               "  A: Coming soon\n"                  +
               "\n"                                  +
               "  Q: What denominations can\n"       +
               "     be dispensed?\n"                +
               "  A: Coming soon\n"                  +
               "\n"                                  +
               "  Q: How are failed transactions\n"  +
               "     reversed?\n"                    +
               "  A: Coming soon\n"                  +
               "\n"                                  +
               "  Q: How do I get a receipt?\n"      +
               "  A: Coming soon\n"                  +
               "\n"                                  +
               "  Q: What if the machine\n"          +
               "     times out?\n"                   +
               "  A: Coming soon\n"                  +
               "\n"                                  +
               "  Q: Is audio guidance available?\n" +
               "  A: Coming soon\n"                  +
               "\n"                                  +
               "  Q: Can text be enlarged or\n"      +
               "     high-contrast?\n"               +
               "  A: Coming soon\n"                  +
               "\n"                                  +
               "  Q: Is there a simple mode for\n"   +
               "     older or first-time users?\n"   +
               "  A: Coming soon\n"                  +
               "\n"                                  +
               "  Q: Accessibility support?\n"       +
               "  A: Coming soon\n"                  +
               "================================";
    }

    // -----------------------------------------------------------------------
    // Main menu helper
    // -----------------------------------------------------------------------
    private String mainMenu() {
        return "================================\n" +
               "  MAIN MENU\n"                     +
               "================================\n" +
               "  Dep  = Deposit\n"                +
               "  W/D  = Withdraw\n"               +
               "  Bal  = Check Balance\n"          +
               "  ChP  = Change Password\n"        +
               "  New  = Create Account\n"         +
               "  Fin  = Logout\n"                 +
               "================================"  +
               faq();
    }

    // -----------------------------------------------------------------------
    // Welcome Page
    // -----------------------------------------------------------------------
    public void initialise() {
        setState(STATE_ACCOUNT_NO);
        numberPadInput = "";
        message = "Welcome to Brighton ATM";
        result  = "================================\n" +
                  "  Please enter your account\n"     +
                  "  number using the keypad\n"       +
                  "  then press \"Ent\"\n"            +
                  "================================\n" +
                  "  New user? Press \"New\"\n"       +
                  "  to create an account"            +
                  faq();
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
            result = "================================\n" +
                     "  Goodbye! Have a great day.\n"   +
                     "================================\n" +
                     "  Enter account number to\n"      +
                     "  start a new session\n"          +
                     "  then press \"Ent\""             +
                     faq();
        } else {
            result = "================================\n" +
                     "  Enter your account number\n"    +
                     "  using the keypad\n"             +
                     "  then press \"Ent\"\n"           +
                     "================================\n" +
                     "  New user? Press \"New\""        +
                     faq();
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
                    result  = "================================\n" +
                              "  Now enter your password\n"       +
                              "  using the keypad\n"              +
                              "  then press \"Ent\""              +
                              faq();
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
                    message = "Login Failed: Unknown Account/Password";
                    reset(message);
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
                    result  = "================================\n"    +
                              "  Enter NEW password\n"               +
                              "  (min 6 chars, letters + digits)\n"  +
                              "  then press \"Ent\""                 +
                              faq();
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
                    message = "Invalid Password (min 6 chars, letters + digits)";
                    result  = "================================\n"    +
                              "  Enter NEW password again\n"         +
                              "  (min 6 chars, letters + digits)\n"  +
                              "  then press \"Ent\""                 +
                              faq();
                }
                break;

            case STATE_NEW_ACC_NO:
                if (numberPadInput.equals("") || numberPadInput.length() < 4) {
                    message = "Account Number Too Short (min 4 digits)";
                    result  = "================================\n" +
                              "  Re-enter account number\n"       +
                              "  (min 4 digits)\n"                +
                              "  then press \"Ent\""              +
                              faq();
                } else {
                    newAccNumber = numberPadInput;
                    numberPadInput = "";
                    setState(STATE_NEW_ACC_PW);
                    message = "Account Number Accepted";
                    result  = "================================\n"   +
                              "  Enter a password\n"                 +
                              "  (min 6 chars, letters + digits)\n"  +
                              "  then press \"Ent\""                 +
                              faq();
                }
                break;

            case STATE_NEW_ACC_PW:
                if (numberPadInput.length() < 6) {
                    message = "Password Too Short (min 6 chars)";
                    result  = "================================\n"   +
                              "  Re-enter password\n"                +
                              "  (min 6 chars, letters + digits)\n"  +
                              "  then press \"Ent\""                 +
                              faq();
                } else {
                    newAccPasswd = numberPadInput;
                    numberPadInput = "";
                    setState(STATE_NEW_ACC_TYPE);
                    message = "Password Accepted";
                    result  = "================================\n" +
                              "  Choose account type:\n"          +
                              "  1 = Student\n"                   +
                              "  2 = Prime\n"                     +
                              "  3 = Saving\n"                    +
                              "  4 = Standard\n"                  +
                              "  then press \"Ent\""              +
                              faq();
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
    // Balance
    // -----------------------------------------------------------------------
    public void processBalance() {
        if (state.equals(STATE_LOGGED_IN)) {
            numberPadInput = "";
            message = "Balance Available";
            result  = "================================\n"          +
                      "  Your current balance is:\n"               +
                      "  \u00A3" + bank.getBalance() + "\n"        +
                      "================================\n"          +
                      "  Dep / W/D / ChP / Fin"                    +
                      faq();
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
                    result  = "================================\n"              +
                              "  Withdrawn: \u00A3" + amount + "\n"           +
                              "  New balance: \u00A3" + bank.getBalance() + "\n" +
                              "================================\n"              +
                              "  Dep / W/D / Bal / ChP / Fin"                 +
                              faq();
                } else {
                    message = "Withdrawal Failed: Insufficient Funds";
                    result  = "================================\n"                  +
                              "  Insufficient funds\n"                             +
                              "  Balance: \u00A3" + bank.getBalance() + "\n"       +
                              "================================\n"                  +
                              "  Please enter a new amount"                        +
                              faq();
                }
            } else {
                message = "Invalid Amount";
                result  = "================================\n" +
                          "  Please enter a valid amount\n"  +
                          "  then press W/D"                 +
                          faq();
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
                result  = "================================\n"              +
                          "  Deposited: \u00A3" + amount + "\n"           +
                          "  New balance: \u00A3" + bank.getBalance() + "\n" +
                          "================================\n"              +
                          "  Dep / W/D / Bal / ChP / Fin"                 +
                          faq();
            } else {
                message = "Invalid Amount";
                result  = "================================\n" +
                          "  Please enter a valid amount\n"  +
                          "  then press Dep"                 +
                          faq();
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
            result  = "================================\n" +
                      "  Enter your OLD password\n"      +
                      "  using the keypad\n"             +
                      "  then press \"Ent\"\n"           +
                      "================================\n" +
                      "  Press CLR to clear input"       +
                      faq();
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
            result  = "================================\n" +
                      "  Enter a new account number\n"   +
                      "  (min 4 digits)\n"               +
                      "  then press \"Ent\"\n"           +
                      "================================\n" +
                      "  Press CLR to clear input"       +
                      faq();
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
