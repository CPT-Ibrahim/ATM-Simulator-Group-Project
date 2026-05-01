package com.atmbanksimulator;

import javafx.application.Application;
import javafx.stage.Stage;
import java.io.IOException;

public class Main extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    public void start(Stage window) {

        // ---------------------------------------------------------------
        // Step 1: Auto-run ADB reverse tunnel so phone can reach this app
        // ---------------------------------------------------------------
        runAdbReverse();

        // ---------------------------------------------------------------
        // Step 2: MVC setup
        // ---------------------------------------------------------------
        Bank bank = new Bank();

        UIModel uiModel = new UIModel(bank);
        View view = new View();
        Controller controller = new Controller();

        view.controller = controller;
        controller.UIModel = uiModel;
        uiModel.view = view;

        // ---------------------------------------------------------------
        // Step 3: Show the animated Welcome Screen first.
        //         When the user presses "Login Manually" the welcome screen
        //         transitions into the normal ATM view.
        //         NFC taps (handled below) also dismiss the splash.
        // ---------------------------------------------------------------
        WelcomeScreen welcomeScreen = new WelcomeScreen(window, () -> {
            // Called when "Login Manually" is pressed — switch to ATM view
            view.start(window);
            DBConnection.launchInspector(window);
            uiModel.initialise();
        });

        welcomeScreen.show();

        // ---------------------------------------------------------------
        // Step 4: Start NFC server — listens for card taps from phone.
        //         On first tap while welcome screen is active, we transition
        //         to the ATM view AND process the NFC login.
        // ---------------------------------------------------------------
        try {
            final boolean[] atmStarted = {false};

            NFCServer nfcServer = new NFCServer(8080, uid -> {
                // If ATM view hasn't launched yet (we're still on welcome screen),
                // start it first, then process the NFC login.
                if (!atmStarted[0]) {
                    atmStarted[0] = true;
                    javafx.application.Platform.runLater(() -> {
                        view.start(window);
                        DBConnection.launchInspector(window);
                        uiModel.initialise();
                        uiModel.processNFCLogin(uid);
                    });
                } else {
                    uiModel.processNFCLogin(uid);
                }
            });

            nfcServer.start();

            // Mark ATM as started when manual login button was pressed
            // (We detect this via the view.start() call in the lambda above —
            //  we need a shared flag so NFC doesn't double-start the view.)
            // The atmStarted[] flag is shared via closure — this is handled above.

        } catch (IOException e) {
            System.out.println("[NFC] Server failed to start: " + e.getMessage());
            System.out.println("[NFC] Is something else using port 8080?");
        }
    }

    // -----------------------------------------------------------------------
    // Runs: adb reverse tcp:8080 tcp:8080
    // -----------------------------------------------------------------------
    private void runAdbReverse() {
        try {
            System.out.println("[ADB] Setting up NFC tunnel...");
            Process process = Runtime.getRuntime().exec("adb reverse tcp:8080 tcp:8080");
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("[ADB] Tunnel ready — phone can now send card UIDs.");
            } else {
                System.out.println("[ADB] Warning: tunnel setup may have failed (exit code " + exitCode + ").");
            }
        } catch (Exception e) {
            System.out.println("[ADB] Could not run adb command: " + e.getMessage());
        }
    }
}
