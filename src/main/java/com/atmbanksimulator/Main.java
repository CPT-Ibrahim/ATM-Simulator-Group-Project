package com.atmbanksimulator;

import javafx.application.Application;
import javafx.stage.Stage;
import java.io.IOException;

public class Main extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage window) {

        // ---------------------------------------------------------------
        // Step 1: ADB reverse tunnel so phone NFC app can reach this host
        // ---------------------------------------------------------------
        runAdbReverse();

        // ---------------------------------------------------------------
        // Step 2: MVC wiring
        // ---------------------------------------------------------------
        Bank       bank       = new Bank();
        UIModel    uiModel    = new UIModel(bank);
        View       view       = new View();
        Controller controller = new Controller();

        view.controller   = controller;
        controller.UIModel = uiModel;
        uiModel.view       = view;

        // ---------------------------------------------------------------
        // Step 3: Hand the Stage directly to View.
        //         View shows the welcome splash, owns the window forever —
        //         no window close/reopen, no size change.
        //         onManualLogin lambda calls uiModel.initialise() which
        //         triggers the manual-login flow inside the same window.
        // ---------------------------------------------------------------
        view.initAndShowWelcome(window, () -> {
            // "Login Manually" pressed on the splash — switch to manual login flow
            uiModel.startManualLoginFlow();
        });

        // Open the DB inspector beside the app window
        DBConnection.launchInspector(window);

        // ---------------------------------------------------------------
        // Step 4: NFC server — card taps arrive here and are forwarded to
        //         UIModel, which shows the welcome greeting then the menu.
        // ---------------------------------------------------------------
        try {
            final boolean[] atmStarted = {false};

            NFCServer nfcServer = new NFCServer(8080, uid -> {
                if (!atmStarted[0]) {
                    atmStarted[0] = true;
                    javafx.application.Platform.runLater(() -> {
                        // UIModel is already wired; just process the NFC tap
                        uiModel.processNFCLogin(uid);
                    });
                } else {
                    uiModel.processNFCLogin(uid);
                }
            });

            nfcServer.start();

        } catch (IOException e) {
            System.out.println("[NFC] Server failed to start: " + e.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    private void runAdbReverse() {
        try {
            System.out.println("[ADB] Setting up NFC tunnel...");
            Process process = Runtime.getRuntime().exec("adb reverse tcp:8080 tcp:8080");
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("[ADB] Tunnel ready.");
            } else {
                System.out.println("[ADB] Warning: tunnel setup may have failed (exit=" + exitCode + ").");
            }
        } catch (Exception e) {
            System.out.println("[ADB] Could not run adb: " + e.getMessage());
        }
    }
}