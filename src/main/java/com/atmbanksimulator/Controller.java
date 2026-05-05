package com.atmbanksimulator;

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
            case "W/D":
                UIModel.processWithdraw();
                break;
            case "Dep":
                UIModel.processDeposit();
                break;
            case "Bal":
                UIModel.processBalance();
                break;
            case "Fin":
                UIModel.processFinish();
                break;
            case "ChP":
                UIModel.processChangePassword();
                break;
            case "New":
                UIModel.processNewAccount();
                break;
            case "Tra":
                UIModel.processTransfer();
                break;
            case "Mut":
                UIModel.processMuteToggle();
                break;
            case "Smt":
                UIModel.processMiniStatement();
                break;
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
            // --- ADD THESE TWO CASES ---
            case "W500":
                UIModel.processQuickWithdraw(500);
                break;
            case "Other":
                UIModel.processOtherWithdraw();
                break;
            // ---------------------------
            case "FAQ":
                break;
            default:
                UIModel.processUnknownKey(action);
                break;
        }
    }
}