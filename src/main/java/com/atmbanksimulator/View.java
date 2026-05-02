package com.atmbanksimulator;

import javafx.animation.*;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.ArcType;
import javafx.scene.text.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// ===== View =====
// Single window, single scene. Background orbs live forever.
// Pages (welcome, login-manual, login-nfc, main-menu, action) fade in/out.
// The numpad is shown only when the current state needs numeric input.
class View {

    // ── Window dimensions — runtime values filled after maximise ───────────
    static int W = 1280;   // sensible fallback; overwritten when scene is known
    static int H = 800;

    Controller controller;

    // ── Scene graph roots ───────────────────────────────────────────────────
    private Stage     stage;
    private StackPane root;
    private Canvas    bgCanvas;

    // ── "Page" container — only one page is visible at a time ──────────────
    private StackPane pageLayer;

    // ── Persistent UI elements (always in DOM, shown/hidden) ───────────────
    private Label      soundBtn;   // tiny mute indicator top-right
    private Button     soundButton;

    // ── Quick-withdraw bar (bottom, only when logged in) ────────────────────
    private HBox       quickBar;
    private Button     quick10, quick20, quick50, quick100;

    // ── Numpad overlay (slides in/out from bottom when needed) ──────────────
    private VBox       numpadOverlay;
    private GridPane   numpadGrid;
    private TextField  numpadField;   // mirrored display inside numpad
    private boolean    numpadVisible = false;

    // ── Main ATM panel (shown after login) ──────────────────────────────────
    private VBox       atmPanel;
    private Label      laMsg;
    private TextField  tfInput;       // hidden input tracker (updated by UIModel)
    private TextArea   taResult;

    // ── Background animation ─────────────────────────────────────────────────
    private AnimationTimer orbTimer;
    private final List<Orb> orbs = new ArrayList<>();
    private final Random rng = new Random(42);

    // ── State ────────────────────────────────────────────────────────────────
    private boolean atmPanelShowing = false;

    // ==========================================================================
    // INIT — called once from Main.java  instead of separate WelcomeScreen.show()
    // ==========================================================================
    public void initAndShowWelcome(Stage window, Runnable onManualLogin) {
        this.stage = window;

        // ── Background canvas — size bound to scene at runtime ────────────
        bgCanvas = new Canvas(W, H);
        initOrbs();
        startOrbAnimation();

        // ── Page layer (content swapped per state) ─────────────────────────
        pageLayer = new StackPane();
        pageLayer.setPickOnBounds(false);

        // ── Numpad overlay (hidden by default) ────────────────────────────
        buildNumpadOverlay();
        numpadOverlay.setVisible(false);
        numpadOverlay.setOpacity(0);

        // ── Quick-withdraw bar ─────────────────────────────────────────────
        buildQuickBar();
        quickBar.setVisible(false);
        quickBar.setOpacity(0);

        // ── Sound button (always top-right) ───────────────────────────────
        soundButton = new Button("🔊");
        soundButton.setId("soundButton");
        soundButton.setOnAction(e -> controller.process("Mut"));
        StackPane.setAlignment(soundButton, Pos.TOP_RIGHT);
        StackPane.setMargin(soundButton, new Insets(14, 18, 0, 0));

        // ── Root assembly ──────────────────────────────────────────────────
        root = new StackPane(bgCanvas, pageLayer, numpadOverlay, quickBar, soundButton);

        // Scene with no fixed size — fills whatever the maximised window gives us
        Scene scene = new Scene(root);
        URL cssUrl = getClass().getResource("/atm.css");
        if (cssUrl != null) scene.getStylesheets().add(cssUrl.toExternalForm());

        // Bind canvas dimensions to the scene so the orb background always fills it
        bgCanvas.widthProperty().bind(scene.widthProperty());
        bgCanvas.heightProperty().bind(scene.heightProperty());

        // Also update our W/H fields when the scene resizes (used by orb positions)
        scene.widthProperty().addListener((obs, o, n)  -> W = n.intValue());
        scene.heightProperty().addListener((obs, o, n) -> H = n.intValue());

        window.setScene(scene);
        window.setTitle("Horizon Bank™");
        window.setResizable(true);
        // Maximise and go fullscreen
        window.setMaximized(true);
        window.setFullScreen(true);
        window.setFullScreenExitHint(""); // suppress the "press Esc" overlay text
        window.show();

        // Allow Escape to toggle fullscreen on/off
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                window.setFullScreen(!window.isFullScreen());
            }
        });

        // ── Show the welcome splash page ───────────────────────────────────
        showWelcomePage(onManualLogin);
    }

    // ==========================================================================
    // WELCOME PAGE  (NFC tap prompt + "Login Manually" button)
    // ==========================================================================
    private void showWelcomePage(Runnable onManualLogin) {

        // ── "Welcome to" / "Horizon Bank™" title ──────────────────────────
        Text welcomeTo = new Text("Welcome to");
        welcomeTo.setFont(Font.font("Georgia", FontWeight.NORMAL, 22));
        welcomeTo.setFill(Color.web("#F5ECD7"));

        Text bankName = new Text("Horizon Bank™");
        bankName.setFont(Font.font("Georgia", FontWeight.BOLD, 52));
        bankName.setFill(Color.web("#D4A843"));
        bankName.setStyle("-fx-effect: dropshadow(gaussian,rgba(0,0,0,.7),18,.3,0,3);");

        Region sep = new Region();
        sep.setPrefWidth(300); sep.setPrefHeight(2);
        sep.setStyle("-fx-background-color: linear-gradient(to right,transparent,#D4A843,transparent);");

        VBox titleBox = new VBox(6, welcomeTo, bankName, sep);
        titleBox.setAlignment(Pos.CENTER);

        // ── NFC GIF / icon ─────────────────────────────────────────────────
        ImageView gifView = loadGif();
        gifView.setFitWidth(140); gifView.setFitHeight(140);
        gifView.setPreserveRatio(true);
        startFloat(gifView);

        // ── Instruction text ───────────────────────────────────────────────
        Text instruction = new Text("Tap your NFC card to log in");
        instruction.setFont(Font.font("Georgia", FontWeight.NORMAL, 18));
        instruction.setFill(Color.web("#F5ECD7"));
        instruction.setTextAlignment(TextAlignment.CENTER);
        startPulse(instruction);

        VBox nfcBox = new VBox(18, gifView, instruction);
        nfcBox.setAlignment(Pos.CENTER);

        // ── Manual login button ────────────────────────────────────────────
        Button manualBtn = new Button("Login Manually");
        manualBtn.setId("manualLoginBtn");
        styleGhostButton(manualBtn);
        manualBtn.setOnAction(e -> {
            SoundPlayer.playButtonPress();
            onManualLogin.run();   // UIModel.initialise() will call showManualLoginFlow()
        });

        // ── "New account" link ─────────────────────────────────────────────
        Button newAccBtn = new Button("Create Account");
        newAccBtn.setId("manualLoginBtn");
        styleGhostButton(newAccBtn);
        newAccBtn.setOnAction(e -> controller.process("New Account"));

        HBox bottomBtns = new HBox(20, manualBtn, newAccBtn);
        bottomBtns.setAlignment(Pos.CENTER);

        // ── Assemble page ──────────────────────────────────────────────────
        VBox page = new VBox(32, titleBox, nfcBox, bottomBtns);
        page.setAlignment(Pos.CENTER);
        page.setMaxWidth(560);
        page.setOpacity(0);

        // ── Animate in ────────────────────────────────────────────────────
        replacePage(page, true);

        // Title fades in first, then fades out, then nfc + buttons fade in
        FadeTransition ftTitle = fade(titleBox, 0, 1, 1200);
        PauseTransition hold   = new PauseTransition(Duration.millis(2200));
        FadeTransition ftTitleOut = fade(titleBox, 1, 0, 900);
        FadeTransition ftNfc   = fade(nfcBox, 0, 1, 900);
        FadeTransition ftBtns  = fade(bottomBtns, 0, 1, 700);

        titleBox.setOpacity(0); nfcBox.setOpacity(0); bottomBtns.setOpacity(0);

        SequentialTransition seq = new SequentialTransition(
                new PauseTransition(Duration.millis(300)),
                ftTitle, hold, ftTitleOut,
                new ParallelTransition(ftNfc, ftBtns)
        );
        seq.play();
    }

    // ==========================================================================
    // Called by UIModel when "Login Manually" is pressed
    // ==========================================================================
    public void showManualLoginAccNumber() {
        showInputPromptPage(
                "Enter Account Number",
                "Type your account number\nthen press  Ent  ↵",
                true
        );
    }

    public void showManualLoginPassword() {
        showInputPromptPage(
                "Enter Password",
                "Type your password\nthen press  Ent  ↵",
                true
        );
    }

    // Generic input-prompt page (used for manual login steps)
    private void showInputPromptPage(String title, String subtitle, boolean showNumpad) {
        Text titleTxt = new Text(title);
        titleTxt.setFont(Font.font("Georgia", FontWeight.BOLD, 30));
        titleTxt.setFill(Color.web("#D4A843"));
        titleTxt.setStyle("-fx-effect: dropshadow(gaussian,rgba(0,0,0,.6),10,.2,0,2);");

        Text subTxt = new Text(subtitle);
        subTxt.setFont(Font.font("Georgia", FontWeight.NORMAL, 16));
        subTxt.setFill(Color.web("#F5ECD7"));
        subTxt.setTextAlignment(TextAlignment.CENTER);
        subTxt.setOpacity(0.8);

        // Small logo watermark
        Text logo = new Text("HORIZON BANK™");
        logo.setFont(Font.font("Georgia", FontWeight.BOLD, 11));
        logo.setFill(Color.web("#D4A843", 0.5));

        VBox page = new VBox(20, logo, titleTxt, subTxt);
        page.setAlignment(Pos.CENTER);
        page.setTranslateY(-60);  // push up to give room for numpad
        page.setMaxWidth(480);

        replacePage(page, true);
        if (showNumpad) setNumpadVisible(true);
    }

    // ==========================================================================
    // Welcome back / NFC login greeting  (transient, then main menu)
    // ==========================================================================
    public void showWelcomeGreeting(String accountType, Runnable afterGreeting) {
        Text hi   = new Text("Welcome!");
        hi.setFont(Font.font("Georgia", FontWeight.NORMAL, 24));
        hi.setFill(Color.web("#F5ECD7"));

        Text acct = new Text(accountType.toUpperCase() + " ACCOUNT");
        acct.setFont(Font.font("Georgia", FontWeight.BOLD, 44));
        acct.setFill(Color.web("#D4A843"));
        acct.setStyle("-fx-effect: dropshadow(gaussian,rgba(0,0,0,.7),18,.3,0,3);");

        Text tagline = new Text("✓  Authenticated");
        tagline.setFont(Font.font("Georgia", FontWeight.NORMAL, 16));
        tagline.setFill(Color.web("#82C882"));

        VBox page = new VBox(14, hi, acct, tagline);
        page.setAlignment(Pos.CENTER);

        setNumpadVisible(false);

        replacePage(page, true);

        // Hold for 2 seconds, then fade out and show main ATM panel
        PauseTransition hold = new PauseTransition(Duration.millis(1800));
        hold.setOnFinished(e -> {
            FadeTransition ft = fade(page, 1, 0, 700);
            ft.setOnFinished(e2 -> afterGreeting.run());
            ft.play();
        });
        hold.play();
    }

    // ==========================================================================
    // MAIN ATM PANEL  (shown after greeting, stays until logout)
    // ==========================================================================
    public void showATMPanel() {
        if (atmPanelShowing) return;
        atmPanelShowing = true;

        // ── Build the ATM content panel ──────────────────────────────────
        atmPanel = buildATMContentPanel();
        atmPanel.setOpacity(0);

        replacePage(atmPanel, false);
        fade(atmPanel, 0, 1, 600).play();
    }

    private VBox buildATMContentPanel() {
        // Bank header
        Label bankLabel = new Label("HORIZON BANK™");
        bankLabel.setId("BankNameLabel");
        Label divider = new Label("─────────────────────────────────────────");
        divider.setId("DividerLabel");

        // Message label
        laMsg = new Label("Welcome");
        laMsg.setId("MessageLabel");
        laMsg.setMaxWidth(Double.MAX_VALUE);
        laMsg.setAlignment(Pos.CENTER);

        // Hidden input tracker (keeps UIModel in sync)
        tfInput = new TextField();
        tfInput.setEditable(false);
        tfInput.setVisible(false);
        tfInput.setManaged(false);

        // Result display
        taResult = new TextArea();
        taResult.setEditable(false);
        taResult.setPrefRowCount(7);
        taResult.setId("ResultScreen");
        taResult.setMaxHeight(Double.MAX_VALUE);

        ScrollPane scroll = new ScrollPane(taResult);
        scroll.setFitToWidth(true);
        scroll.setId("ResultScrollPane");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        // ── Real ATM layout: left buttons | screen | right buttons ──────────
        VBox leftBtns  = buildLeftButtonColumn();
        VBox rightBtns = buildRightButtonColumn();

        VBox centerArea = new VBox(6, laMsg, tfInput, scroll);
        centerArea.setAlignment(Pos.TOP_CENTER);
        HBox.setHgrow(centerArea, Priority.ALWAYS);

        HBox mainRow = new HBox(14, leftBtns, centerArea, rightBtns);
        mainRow.setAlignment(Pos.CENTER);
        VBox.setVgrow(mainRow, Priority.ALWAYS);

        // FAQ button
        Button faqBtn = new Button("? FAQ");
        faqBtn.setId("faqButton");
        faqBtn.setOnAction(e -> {
            controller.process("FAQ");
            openFAQWindow();
        });

        HBox bottomRow = new HBox(10, faqBtn);
        bottomRow.setAlignment(Pos.CENTER_RIGHT);

        VBox panel = new VBox(8,
                bankLabel, divider,
                mainRow,
                bottomRow
        );
        panel.setId("ATMPanel");
        panel.setPadding(new Insets(18, 24, 14, 24));
        panel.setMaxWidth(Math.min(1000, W - 80));
        panel.setMaxHeight(H - 60);
        panel.setPrefWidth(Math.min(1000, W - 80));
        panel.setAlignment(Pos.TOP_CENTER);
        return panel;
    }

    // ── Left column: Deposit · Withdraw · Balance · Transfer ─────────────────
    private VBox buildLeftButtonColumn() {
        Button dep = createSideButton("Deposit",  "action-btn-gold",  Pos.CENTER_LEFT);
        Button wd  = createSideButton("Withdraw", "action-btn-gold",  Pos.CENTER_LEFT);
        Button bal = createSideButton("Balance",  "action-btn-gold",  Pos.CENTER_LEFT);
        Button tra = createSideButton("Transfer", "action-btn-gold",  Pos.CENTER_LEFT);

        VBox col = new VBox(10, dep, wd, bal, tra);
        col.setAlignment(Pos.CENTER_LEFT);
        col.setPrefWidth(130);
        return col;
    }

    // ── Right column: Statement · Change PIN · Logout ───────────────────────
    private VBox buildRightButtonColumn() {
        Button smt = createSideButton("Statement",  "action-btn-gold",  Pos.CENTER_RIGHT);
        Button chp = createSideButton("Change PIN", "action-btn-cream", Pos.CENTER_RIGHT);
        Button fin = createSideButton("Logout",     "action-btn-red",   Pos.CENTER_RIGHT);

        VBox col = new VBox(10, smt, chp, fin);
        col.setAlignment(Pos.CENTER_RIGHT);
        col.setPrefWidth(130);
        return col;
    }

    private Button createSideButton(String text, String styleClass, Pos alignment) {
        Button btn = new Button(text);
        btn.setOnAction(this::buttonClicked);
        btn.getStyleClass().addAll("atm-button", styleClass);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(alignment);
        return btn;
    }

    // ==========================================================================
    // PUBLIC UPDATE HOOKS  (called by UIModel)
    // ==========================================================================
    public void update(String msg, String input, String result) {
        if (laMsg   != null) laMsg.setText(msg);
        if (tfInput != null) tfInput.setText(input);
        if (taResult != null) taResult.setText(result);
        // Mirror the input into the numpad display field
        if (numpadField != null) numpadField.setText(input);
    }

    public void setSoundMuted(boolean muted) {
        if (soundButton != null)
            soundButton.setText(muted ? "🔇" : "🔊");
    }

    public void setQuickButtonsVisible(boolean visible) {
        if (quickBar == null) return;
        if (visible && !quickBar.isVisible()) {
            quickBar.setVisible(true);
            fade(quickBar, 0, 1, 400).play();
        } else if (!visible && quickBar.isVisible()) {
            FadeTransition ft = fade(quickBar, 1, 0, 300);
            ft.setOnFinished(e -> quickBar.setVisible(false));
            ft.play();
        }
    }

    /**
     * Called by UIModel to show/hide the numpad depending on current state.
     * States that need number input: withdraw amount, deposit amount,
     * transfer amount, transfer dest, change password, manual login,
     * new account number/password/type.
     */
    public void setNumpadVisible(boolean show) {
        if (show == numpadVisible) return;
        numpadVisible = show;
        if (show) {
            numpadOverlay.setVisible(true);
            fade(numpadOverlay, 0, 1, 350).play();
            if (atmPanel != null) {
                // Shrink result area to give room
                TranslateTransition tt = new TranslateTransition(Duration.millis(300), atmPanel);
                tt.setToY(-60);
                tt.play();
            }
        } else {
            FadeTransition ft = fade(numpadOverlay, 1, 0, 300);
            ft.setOnFinished(e -> numpadOverlay.setVisible(false));
            ft.play();
            if (atmPanel != null) {
                TranslateTransition tt = new TranslateTransition(Duration.millis(300), atmPanel);
                tt.setToY(0);
                tt.play();
            }
        }
    }

    /** Reset the entire view to welcome state (after logout) */
    public void resetToWelcome(Runnable onManualLogin) {
        atmPanelShowing = false;
        setNumpadVisible(false);
        setQuickButtonsVisible(false);
        showWelcomePage(onManualLogin);
    }

    // ==========================================================================
    // NUMPAD OVERLAY (slides in from bottom)
    // ==========================================================================
    private void buildNumpadOverlay() {
        // Display field at top of numpad
        numpadField = new TextField();
        numpadField.setEditable(false);
        numpadField.setId("NumpadField");
        numpadField.setMaxWidth(220);
        numpadField.setAlignment(Pos.CENTER_RIGHT);

        // Grid of buttons
        numpadGrid = new GridPane();
        numpadGrid.setHgap(8);
        numpadGrid.setVgap(8);
        numpadGrid.setAlignment(Pos.CENTER);

        String[][] keys = {
                {"7","8","9"},
                {"4","5","6"},
                {"1","2","3"},
                {"CLR","0","Ent"}
        };
        for (int row = 0; row < keys.length; row++) {
            for (int col = 0; col < keys[row].length; col++) {
                String k = keys[row][col];
                Button btn;
                if (k.equals("CLR")) {
                    btn = createColoredGridButton(k, "action-btn-red");
                } else if (k.equals("Ent")) {
                    btn = createColoredGridButton(k, "action-btn-green");
                } else {
                    btn = createNumpadButton(k);
                }
                numpadGrid.add(btn, col, row);
            }
        }

        VBox inner = new VBox(10, numpadField, numpadGrid);
        inner.setAlignment(Pos.CENTER);
        inner.setPadding(new Insets(14, 20, 14, 20));
        inner.setStyle(
                "-fx-background-color: rgba(20,9,2,0.88);" +
                        "-fx-background-radius: 18px 18px 0 0;" +
                        "-fx-border-color: rgba(212,168,67,0.5);" +
                        "-fx-border-width: 1.5 1.5 0 1.5;" +
                        "-fx-border-radius: 18px 18px 0 0;"
        );

        numpadOverlay = new VBox(inner);
        numpadOverlay.setAlignment(Pos.BOTTOM_CENTER);
        StackPane.setAlignment(numpadOverlay, Pos.BOTTOM_CENTER);
        numpadOverlay.setPickOnBounds(false);
    }

    // ==========================================================================
    // QUICK WITHDRAW BAR
    // ==========================================================================
    private void buildQuickBar() {
        quick10  = quickBtn("£10",  "W10");
        quick20  = quickBtn("£20",  "W20");
        quick50  = quickBtn("£50",  "W50");
        quick100 = quickBtn("£100", "W100");

        // "Other Amount" — opens numpad for a custom withdrawal
        Button quickOther = new Button("Other Amount");
        quickOther.setId("quickBtn");
        quickOther.setStyle(
                "-fx-font-family:'Georgia';-fx-font-size:12px;-fx-font-weight:bold;" +
                        "-fx-background-color:linear-gradient(to bottom,rgba(245,236,215,.88),rgba(210,190,155,.88));" +
                        "-fx-text-fill:#3B1F08;" +
                        "-fx-border-color:rgba(160,120,70,.7);-fx-border-width:1.5px;" +
                        "-fx-background-radius:8px;-fx-border-radius:8px;" +
                        "-fx-min-width:100px;-fx-pref-height:36px;-fx-cursor:hand;"
        );
        quickOther.setOnAction(e -> controller.process("WOther"));

        quickBar = new HBox(10, quick10, quick20, quick50, quick100, quickOther);
        quickBar.setAlignment(Pos.CENTER);
        quickBar.setPadding(new Insets(0, 0, 14, 0));
        StackPane.setAlignment(quickBar, Pos.BOTTOM_CENTER);
        quickBar.setPickOnBounds(false);
    }

    private Button quickBtn(String label, String action) {
        Button b = new Button(label);
        b.setId("quickBtn");
        b.setOnAction(e -> controller.process(action));
        return b;
    }

    // ==========================================================================
    // PAGE TRANSITION HELPERS
    // ==========================================================================
    private void replacePage(Node newPage, boolean animate) {
        if (animate) {
            // Fade out old page, fade in new
            List<Node> old = new ArrayList<>(pageLayer.getChildren());
            pageLayer.getChildren().setAll(newPage);
            newPage.setOpacity(0);
            FadeTransition ftIn = fade(newPage, 0, 1, 600);
            if (!old.isEmpty()) {
                FadeTransition ftOut = fade(old.get(0), 1, 0, 350);
                ftOut.setOnFinished(e -> ftIn.play());
                ftOut.play();
            } else {
                ftIn.play();
            }
        } else {
            pageLayer.getChildren().setAll(newPage);
        }
    }

    // ==========================================================================
    // BUTTON FACTORIES
    // ==========================================================================
    private Button createActionButton(String text, String styleClass) {
        Button btn = new Button(text);
        btn.setOnAction(this::buttonClicked);
        btn.getStyleClass().addAll("atm-button", styleClass);
        btn.setPrefWidth(90);
        return btn;
    }

    private Button createNumpadButton(String text) {
        Button btn = new Button(text);
        btn.setOnAction(this::buttonClicked);
        btn.getStyleClass().addAll("atm-button", "keypad-btn");
        btn.setPrefWidth(64); btn.setPrefHeight(52);
        return btn;
    }

    private Button createColoredGridButton(String text, String styleClass) {
        Button btn = new Button(text);
        btn.setOnAction(this::buttonClicked);
        btn.getStyleClass().addAll("atm-button", styleClass);
        btn.setPrefWidth(64); btn.setPrefHeight(52);
        return btn;
    }

    private void buttonClicked(ActionEvent event) {
        controller.process(((Button) event.getSource()).getText());
    }

    private void styleGhostButton(Button btn) {
        btn.setStyle(
                "-fx-font-family:'Georgia';" +
                        "-fx-font-size:13px;" +
                        "-fx-text-fill:#F5ECD7;" +
                        "-fx-background-color:rgba(107,66,38,0.7);" +
                        "-fx-background-radius:8px;" +
                        "-fx-border-color:#D4A843;" +
                        "-fx-border-width:1.5px;" +
                        "-fx-border-radius:8px;" +
                        "-fx-padding:8 22 8 22;" +
                        "-fx-cursor:hand;"
        );
        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-font-family:'Georgia';" +
                        "-fx-font-size:13px;" +
                        "-fx-text-fill:#D4A843;" +
                        "-fx-background-color:rgba(107,66,38,0.95);" +
                        "-fx-background-radius:8px;" +
                        "-fx-border-color:#D4A843;" +
                        "-fx-border-width:1.5px;" +
                        "-fx-border-radius:8px;" +
                        "-fx-padding:8 22 8 22;" +
                        "-fx-cursor:hand;"
        ));
        btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-font-family:'Georgia';" +
                        "-fx-font-size:13px;" +
                        "-fx-text-fill:#F5ECD7;" +
                        "-fx-background-color:rgba(107,66,38,0.7);" +
                        "-fx-background-radius:8px;" +
                        "-fx-border-color:#D4A843;" +
                        "-fx-border-width:1.5px;" +
                        "-fx-border-radius:8px;" +
                        "-fx-padding:8 22 8 22;" +
                        "-fx-cursor:hand;"
        ));
    }

    // ==========================================================================
    // ANIMATION HELPERS
    // ==========================================================================
    private FadeTransition fade(Node node, double from, double to, int ms) {
        FadeTransition ft = new FadeTransition(Duration.millis(ms), node);
        ft.setFromValue(from);
        ft.setToValue(to);
        return ft;
    }

    private void startFloat(Node node) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(2200), node);
        tt.setFromY(0); tt.setToY(-12);
        tt.setAutoReverse(true);
        tt.setCycleCount(Animation.INDEFINITE);
        tt.setInterpolator(Interpolator.EASE_BOTH);
        tt.play();
    }

    private void startPulse(Node node) {
        FadeTransition ft = new FadeTransition(Duration.millis(1800), node);
        ft.setFromValue(0.55); ft.setToValue(1.0);
        ft.setAutoReverse(true);
        ft.setCycleCount(Animation.INDEFINITE);
        ft.play();
    }

    // ==========================================================================
    // BACKGROUND ORB ANIMATION (runs the whole session)
    // ==========================================================================
    private void initOrbs() {
        String[] colours = {"#C8A882","#A07850","#D4A843","#8B5E3C","#E8C99A","#6B4226"};
        for (int i = 0; i < 8; i++) {
            Orb o = new Orb();
            o.x      = rng.nextDouble() * W;
            o.y      = rng.nextDouble() * H;
            o.radius = 110 + rng.nextDouble() * 200;
            o.dx     = (rng.nextDouble() - 0.5) * 0.38;
            o.dy     = (rng.nextDouble() - 0.5) * 0.38;
            o.alpha  = 0.15 + rng.nextDouble() * 0.19;
            o.colour = colours[i % colours.length];
            orbs.add(o);
        }
    }

    private void startOrbAnimation() {
        GraphicsContext gc = bgCanvas.getGraphicsContext2D();
        orbTimer = new AnimationTimer() {
            @Override public void handle(long now) {
                LinearGradient bg = new LinearGradient(0,0,0,1,true, CycleMethod.NO_CYCLE,
                        new Stop(0.0, Color.web("#3B2314")),
                        new Stop(0.5, Color.web("#5C3317")),
                        new Stop(1.0, Color.web("#2A1508")));
                gc.setFill(bg);
                gc.fillRect(0, 0, W, H);
                for (Orb o : orbs) {
                    RadialGradient g = new RadialGradient(0,0,
                            o.x/W, o.y/H, o.radius/Math.max(W,H),
                            true, CycleMethod.NO_CYCLE,
                            new Stop(0.0, Color.web(o.colour, o.alpha)),
                            new Stop(1.0, Color.web(o.colour, 0.0)));
                    gc.setFill(g);
                    gc.fillOval(o.x-o.radius, o.y-o.radius, o.radius*2, o.radius*2);
                    o.x += o.dx; o.y += o.dy;
                    if (o.x < -o.radius || o.x > W+o.radius) o.dx = -o.dx;
                    if (o.y < -o.radius || o.y > H+o.radius) o.dy = -o.dy;
                }
            }
        };
        orbTimer.start();
    }

    private static class Orb {
        double x,y,radius,dx,dy,alpha; String colour;
    }

    // ==========================================================================
    // GIF / NFC ICON  (reused from old WelcomeScreen)
    // ==========================================================================
    private ImageView loadGif() {
        String[] locations = {"/Tap_card_animation.gif","file:/mnt/user-data/uploads/Tap_card_animation.gif"};
        for (String loc : locations) {
            try {
                URL url = loc.startsWith("file:") ? new java.net.URL(loc) : getClass().getResource(loc);
                if (url != null) {
                    ImageView iv = new ImageView(new Image(url.toExternalForm(), true));
                    return iv;
                }
            } catch (Exception ignored) {}
        }
        return buildFallbackNFCIcon();
    }

    private ImageView buildFallbackNFCIcon() {
        Canvas c = new Canvas(160, 160);
        GraphicsContext gc = c.getGraphicsContext2D();
        gc.setStroke(Color.web("#D4A843")); gc.setLineWidth(4);
        double cx=80, cy=80;
        for (int r=20; r<=70; r+=18)
            gc.strokeArc(cx-r, cy-r, r*2, r*2, -45, 90, ArcType.OPEN);
        gc.setFill(Color.web("#D4A843",0.25));
        gc.fillRoundRect(25,65,55,38,6,6);
        gc.setStroke(Color.web("#D4A843")); gc.setLineWidth(2);
        gc.strokeRoundRect(25,65,55,38,6,6);
        javafx.scene.image.WritableImage wi = new javafx.scene.image.WritableImage(160,160);
        c.snapshot(null, wi);
        return new ImageView(wi);
    }

    // ==========================================================================
    // FAQ window
    // ==========================================================================
    private void openFAQWindow() {
        Stage s = new Stage();
        s.setTitle("FAQ – Horizon Bank ATM");
        TextArea t = new TextArea(FAQ_TEXT);
        t.setEditable(false); t.setWrapText(true); t.setPrefSize(400,500);
        t.setStyle("-fx-font-family:'Georgia';-fx-font-size:12px;" +
                "-fx-background-color:#2A1508;-fx-text-fill:#F5ECD7;");
        s.setScene(new Scene(t, 420, 520));
        s.show();
    }

    // ==========================================================================
    // KEPT FOR COMPATIBILITY — old start() path is no longer used.
    // Main.java should call initAndShowWelcome() directly.
    // ==========================================================================
    /** @deprecated Use initAndShowWelcome() */
    public void start(Stage window) {
        // no-op — Main.java now calls initAndShowWelcome()
    }

    // ==========================================================================
    // FAQ TEXT
    // ==========================================================================
    private static final String FAQ_TEXT =
            "HOW TO USE THE ATM\n================================\n\n" +
                    "Q: How do I log in?\nA: Tap your NFC card, or press\n   'Login Manually' and enter\n   your account number + password\n\n" +
                    "Q: How do I withdraw cash?\nA: Press W/D or use quick buttons\n   £10 £20 £50 £100\n\n" +
                    "Q: How do I check my balance?\nA: Press Bal when logged in\n\n" +
                    "Q: How do I change my PIN?\nA: Press ChP when logged in\n\n" +
                    "Q: How do I deposit?\nA: Enter amount then press Dep\n\n" +
                    "Q: How do I logout?\nA: Press Fin when logged in\n\n" +
                    "Q: How do I create an account?\nA: Press 'Create Account' on\n   the welcome screen\n\n" +
                    "Q: How do I transfer money?\nA: Press Tra, enter destination\n   account number, then amount\n\n" +
                    "Q: How do I view transactions?\nA: Press Smt for mini statement\n   (last 5 transactions)\n\n" +
                    "Q: Low balance warning?\nA: Shown when balance < £50\n\n" +
                    "================================\n" +
                    "COMING SOON\nReceipts, timeout, accessibility,\naudio guidance, high contrast mode.";
}