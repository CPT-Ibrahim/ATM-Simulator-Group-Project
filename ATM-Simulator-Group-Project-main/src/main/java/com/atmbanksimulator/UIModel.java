package com.atmbanksimulator;

// ===== UIModel (Brain) =====

// The UIModel represents all the actual content and functionality of the app
// For the ATM, it keeps track of the information shown in the display
// (the laMsg and two tfInput boxes), and the interaction with the bank, executes
// commands provided by the controller and tells the view to update when
// something changes
public class UIModel {
    View view; // Reference to the View (part of the MVC setup)
    private Bank bank; // The ATM communicates with this Bank

    // The ATM UIModel can be in one of three states:
    // 1. Waiting for an account number
    // 2. Waiting for a password
    // 3. Logged in (ready to process requests for the logged-in account)
    // We represent each state with a String constant.
    // The 'final' keyword ensures these values cannot be changed.
    private final String STATE_ACCOUNT_NO = "account_no";
    private final String STATE_PASSWORD = "password";
    private final String STATE_LOGGED_IN = "logged_in";
    private final String STATE_CHANGE_PW_OLD  = "change_pw_old";
    private final String STATE_CHANGE_PW_NEW  = "change_pw_new";
    private final String STATE_NEW_ACC_NO     = "new_acc_no";
    private final String STATE_NEW_ACC_PW     = "new_acc_pw";
    private final String STATE_NEW_ACC_TYPE   = "new_acc_type";

    // Temporary storage for new-account creation flow
    private String newAccNumber = "";
    private String newAccPasswd = "";
    // Variables representing the state and data of the ATM UIModel
    private String state = STATE_ACCOUNT_NO;    // Current state of the ATM
    private String accNumber = "";         // Account number being typed
    private String accPasswd = "";         // Password being typed

    // Variables shown on the View display
    private String message;                // Message label text
    private String numberPadInput;         // Current number displayed in the TextField (as a string)
    private String result;                 // Contents of the TextArea (may be multiple lines)

    // UIModel constructor: pass a Bank object that the ATM interacts with
    public UIModel(Bank bank) {
        this.bank = bank;
    }

    // Initialize the ATM UIModel: this method is called by Main when starting the app
    // - Set state to STATE_ACCOUNT_NO
    // - Clear the numberPadInput - numbers displayed in the TextField
    // - Display the welcome message and user instructions
    public void initialise() {
        setState(STATE_ACCOUNT_NO);
        numberPadInput = "";
        message = "Welcome to the ATM";
        result = "Enter your account number\nFollowed by \"Ent\"";
        update();
    }

    // Reset the ATM UIModel after an invalid action or logout:
    // - Set state to STATE_ACCOUNT_NO
    // - Clear the numberPadInput
    // - Display the provided message and user instructions
    private void reset(String msg) {
        setState(STATE_ACCOUNT_NO);
        numberPadInput = "";
        message = msg;
        result = "Enter your account number\nFollowed by \"Ent\"";
    }

    // Change the ATM state and print a debug message whenever the state changes
    private void setState(String newState)
    {
        if ( !state.equals(newState) )
        {
            String oldState = state;
            state = newState;
            System.out.println("UIModel::setState: changed state from "+ oldState + " to " + newState);
        }
    }

    // These process**** methods are called by the Controller
    // in response to specific button presses on the GUI.

    // Handle a number button press: append the digit to numberPadInput
    public void processNumber(String numberOnButton) {
        // Optional extension:
        // Improve feedback by showing what the number is being entered for based on the current state.
        // e.g.  if state is STATE_ACCOUNT_NO, display "Receiving Account Number, Beep 5 received"
        numberPadInput += numberOnButton;
        message = "Beep! " + numberOnButton + " received";
        update();
    }

    // Handle the Clear button: reset the current number stored in numberPadInput
    public void processClear() {
        // Optional extension:
        // Improve feedback by showing what was cleared depending on the current state.
        // e.g. if state is STATE_ACCOUNT_NO, display "Account Number cleared: 123"
        if (!numberPadInput.isEmpty()) {
            numberPadInput = "";
            message = "Input Cleared";
            update();
        }
    }

    // Handle the Enter button.
    // This is a more complex method: pressing Enter causes the ATM to change state,
    // progressing from STATE_ACCOUNT_NO → STATE_PASSWORD → STATE_LOGGED_IN,
    // and back to STATE_ACCOUNT_NO when logging out.
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
                    result = "Now enter your password\nFollowed by \"Ent\"";
                }
                break;

            case STATE_PASSWORD:
                accPasswd = numberPadInput;
                numberPadInput = "";
                if (bank.login(accNumber, accPasswd)) {
                    setState(STATE_LOGGED_IN);
                    message = "Logged In";
                    result = "Dep / W/D / Bal / Fin / ChP / New";
                } else {
                    message = "Login failed: Unknown Account/Password";
                    reset(message);
                }
                break;

            case STATE_CHANGE_PW_OLD:
                // Verify old password matches the in-session credential
                if (numberPadInput.equals("") || !numberPadInput.equals(bank.getLoggedInPassword())) {
                    numberPadInput = "";
                    message = "Incorrect old password";
                    setState(STATE_LOGGED_IN);
                    result = "Dep / W/D / Bal / Fin / ChP / New";
                } else {
                    numberPadInput = "";
                    setState(STATE_CHANGE_PW_NEW);
                    message = "Old password verified";
                    result = "Enter new password (min 6 chars,\nmust include letters and digits)\nThen press \"Ent\"";
                }
                break;

            case STATE_CHANGE_PW_NEW:
                String newPw = numberPadInput;
                numberPadInput = "";
                if (bank.changePassword(newPw)) {
                    setState(STATE_LOGGED_IN);
                    message = "Password changed successfully";
                    result = "Dep / W/D / Bal / Fin / ChP / New";
                } else {
                    message = "Invalid password (min 6 chars, letters + digits required)";
                    result = "Enter new password again\nThen press \"Ent\"";
                    // stay in STATE_CHANGE_PW_NEW so user can retry
                }
                break;

            case STATE_NEW_ACC_NO:
                if (numberPadInput.equals("") || numberPadInput.length() < 4) {
                    message = "Account number too short (min 4 digits)";
                    result = "Re-enter account number\nThen press \"Ent\"";
                } else {
                    newAccNumber = numberPadInput;
                    numberPadInput = "";
                    setState(STATE_NEW_ACC_PW);
                    message = "Account number accepted";
                    result = "Enter a password (min 6 chars,\nletters + digits)\nThen press \"Ent\"";
                }
                break;

            case STATE_NEW_ACC_PW:
                if (numberPadInput.length() < 6) {
                    message = "Password too short (min 6 chars)";
                    result = "Re-enter password\nThen press \"Ent\"";
                } else {
                    newAccPasswd = numberPadInput;
                    numberPadInput = "";
                    setState(STATE_NEW_ACC_TYPE);
                    message = "Password accepted";
                    result = "Enter account type:\n1 = Student  2 = Prime\n3 = Saving   4 = Standard";
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
                    reset("Account created: " + newAccNumber + " (" + accType + ")");
                    newAccNumber = "";
                    newAccPasswd = "";
                } else {
                    reset("Account already exists or creation failed");
                    newAccNumber = "";
                    newAccPasswd = "";
                }
                break;

            case STATE_LOGGED_IN:
            default:
                // no-op
        }
        update(); // Refresh the GUI to show messages and input
    }

    /**
     * Parses a string into a valid transaction amount.
     * - If the string is empty, invalid, or consists only of zeros, returns 0.
     * - Otherwise, returns the integer value.
     *
     * Purpose:
     * Helper method for validating user-entered amounts in transactions (Deposit, Withdraw, etc.).
     *
     * Note: If you later add features like Transfer, this method can be reused.
     */
    private int parseValidAmount(String number) {
        if (number.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(number);
        } catch (NumberFormatException e) {
            return 0; // Invalid input -> treated as 0
        }
    }

    // Handle the Balance button:
    // - If the user is logged in, retrieve the current balance and update messages/results accordingly
    // - Otherwise, reset the ATM and display an error message
    public void processBalance() {
        if (state.equals(STATE_LOGGED_IN) ) {
            numberPadInput = "";
            message = "Balance Available";
            result = "Your Balance is: " + bank.getBalance();
        } else {
            reset("You are not logged in");
        }
        update();
    }

    // Handle the Withdraw button:
    // If the user is logged in, attempt to withdraw the amount entered;
    // otherwise, reset the ATM and display an error message.
    // Reads the amount from numberPadInput, validates it, and updates messages/results accordingly.
    public void processWithdraw() {
        if (state.equals(STATE_LOGGED_IN)) {
            int amount = parseValidAmount(numberPadInput);
            if (amount > 0) {
                if(bank.withdraw( amount )){
                    message = "Withdraw Successful";
                    result = "Withdrawn: " + numberPadInput;
                }
                else{
                    message = "Withdraw Failed: Insufficient Funds";
                    result = "Now enter the amount\nThen press transaction\n(Dep = Deposit, W/D = Withdraw)";
                }
            }
            else{
                message = "Invalid Amount";
                result = "Now enter the amount\nThen press transaction\n(Dep = Deposit, W/D = Withdraw)";
            }
            numberPadInput = "";
        }
        else {
            reset("You are not logged in");
        }
        update();
    }

    // Handle the Deposit button:
    // - If the user is logged in, deposit the amount entered into the bank
    // - Reads the amount from numberPadInput, validates it, and updates messages/results accordingly
    // - Otherwise, reset the ATM and display an error message
    public void processDeposit() {
        if (state.equals(STATE_LOGGED_IN)) {
            int amount = parseValidAmount(numberPadInput);
            if (amount > 0) {
                bank.deposit( amount );
                message = "Deposit Successful";
                result = "Deposited: " + numberPadInput;
            }
            else {
                message = "Invaild Amount";
                result = "Now enter the amount\nThen press transaction\n(Dep = Deposit, W/D = Withdraw)";
            }
            numberPadInput = "";
        }
        else {
            reset("You are not logged in");
        }
        update();
    }

    // Handle the Finish button:
    // - If the user is logged in, log out
    // - Otherwise, reset the ATM and display an error message
    public void processFinish() {
        if (state.equals(STATE_LOGGED_IN) ) {
            reset("Thank you for using the Bank ATM");
            bank.logout();
        } else {
            reset("You are not logged in");
        }
        update();
    }

    // Handle unknown or invalid buttons for the current state:
    // - Reset the ATM and display an "Invalid Command" message
    public void processUnknownKey(String action) {
        reset("Invalid Command");
        update();
    }

    // Notify the View of changes by calling its update method
    private void update() {
        view.update(message,numberPadInput, result);
    }
    // Handle Change Password button (ChP)
    public void processChangePassword() {
        if (state.equals(STATE_LOGGED_IN)) {
            numberPadInput = "";
            setState(STATE_CHANGE_PW_OLD);
            message = "Change Password";
            result = "Enter your OLD password\nThen press \"Ent\"";
        } else {
            reset("You are not logged in");
        }
        update();
    }

    // Handle New Account button (New)
    public void processNewAccount() {
        if (state.equals(STATE_ACCOUNT_NO)) {
            numberPadInput = "";
            newAccNumber = "";
            newAccPasswd = "";
            setState(STATE_NEW_ACC_NO);
            message = "Create New Account";
            result = "Enter a new account number\n(min 4 digits)\nThen press \"Ent\"";
        } else {
            reset("Please log out before creating an account");
        }
        update();
    }
}

