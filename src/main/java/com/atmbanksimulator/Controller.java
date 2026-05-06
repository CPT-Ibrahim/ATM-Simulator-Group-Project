package com.atmbanksimulator;

// ===== Controller (Nerves) =====
// Receives button presses from View and delegates to UIModel.
public class Controller {

    UIModel UIModel;

    void process(String action) {
        SoundPlayer.playButtonPress();

        switch (action) {
            case "1": case "2": case "3": case "4": case "5":
            case "6": case "7": case "8": case "9": case "0":
                UIModel.processNumber(action);
                break;
            case "CLR":
                UIModel.processClear();
                break;
            case "Ent":
                UIModel.processEnter();
                break;
            // ── Full-name action buttons ───────────────────────────────────
            case "Withdraw":
                UIModel.processWithdraw();
                break;
            case "Deposit":
                UIModel.processDeposit();
                break;
            case "Balance":
                UIModel.processBalance();
                break;
            case "Logout":
                UIModel.processFinish();
                break;
            case "Change PIN":
                UIModel.processChangePassword();
                break;
            case "New Account":
                UIModel.processNewAccount();
                break;
            case "Transfer":
                UIModel.processTransfer();
                break;
            case "Statement":
                UIModel.processMiniStatement();
                break;
            case "Mut":
                UIModel.processMuteToggle();
                break;
            case "Go Back":
                UIModel.processGoBack();
                break;
            case "FAQ":
                // View opens FAQ window; sound already played above.
                break;
            // ── Quick withdraw options (shown after Withdraw pressed) ──────
            case "W10":
                UIModel.processQuickWithdraw(10);
                break;
            case "W20":
                UIModel.processQuickWithdraw(20);
                break;
            case "W50":
                UIModel.processQuickWithdraw(50);
                break;
            case "W100":
                UIModel.processQuickWithdraw(100);
                break;
            case "W200":
                UIModel.processQuickWithdraw(200);
                break;
            case "WOther":
                UIModel.processWithdrawOther();
                break;
            default:
                UIModel.processUnknownKey(action);
                break;
        }
    }
}