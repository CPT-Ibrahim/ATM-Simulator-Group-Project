package com.atmbanksimulator;

import javafx.stage.Stage;

/**
 * WelcomeScreen — thin wrapper kept for API compatibility with Main.java.
 *
 * The actual welcome UI now lives entirely inside View so the window
 * never closes or resizes between the splash screen and the ATM session.
 *
 * Main.java creates this, calls show(), which delegates straight to
 * View.initAndShowWelcome().  The View owns the Stage from now on.
 */
public class WelcomeScreen {

    private final Stage    stage;
    private final Runnable onManualLogin;
    private final View     view;

    /**
     * @param stage         the primary Stage (passed in from Main.java)
     * @param onManualLogin called when the user presses "Login Manually"
     */
    public WelcomeScreen(Stage stage, Runnable onManualLogin) {
        this.stage         = stage;
        this.onManualLogin = onManualLogin;
        this.view          = null; // view is wired externally via Main.java
    }

    /**
     * Constructor used when View is already wired (preferred path from Main.java).
     */
    public WelcomeScreen(Stage stage, View view, Runnable onManualLogin) {
        this.stage         = stage;
        this.view          = view;
        this.onManualLogin = onManualLogin;
    }

    /** Opens the welcome screen.  Delegates to View if wired; otherwise no-op. */
    public void show() {
        if (view != null) {
            view.initAndShowWelcome(stage, onManualLogin);
        }
        // If called via the old single-arg constructor (legacy), Main.java
        // calls view.initAndShowWelcome() directly, so nothing is needed here.
    }
}
