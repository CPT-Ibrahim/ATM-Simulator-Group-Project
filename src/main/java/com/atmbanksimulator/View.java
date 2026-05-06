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

/**
 * View
 * -----------------------------------------------------------------------------
 * One stage, one scene, one premium Horizon Bank theme across the whole app.
 * The old console/terminal area has been removed.  Each action now opens its
 * own clean page with instructions, a centred keypad when needed, and a Go Back
 * button so the user can return to the main option menu at any point.
 */
class View {

    static int W = 1280;
    static int H = 800;

    Controller controller;

    private Stage stage;
    private StackPane root;
    private StackPane pageLayer;
    private Canvas bgCanvas;
    private Button soundButton;

    private TextField numpadField;
    private Label pageTitleLabel;
    private Label pageInstructionLabel;
    private Label pageHelperLabel;

    private AnimationTimer orbTimer;
    private final List<Orb> orbs = new ArrayList<>();
    private final Random rng = new Random(42);

    private Runnable manualLoginCallback;

    // =====================================================================
    // INIT
    // =====================================================================
    public void initAndShowWelcome(Stage window, Runnable onManualLogin) {
        this.stage = window;
        this.manualLoginCallback = onManualLogin;

        bgCanvas = new Canvas(W, H);
        initOrbs();
        startOrbAnimation();

        pageLayer = new StackPane();
        pageLayer.setPickOnBounds(false);

        soundButton = new Button("🔊");
        soundButton.setId("soundButton");
        soundButton.setOnAction(e -> controller.process("Mut"));
        StackPane.setAlignment(soundButton, Pos.TOP_RIGHT);
        StackPane.setMargin(soundButton, new Insets(14, 18, 0, 0));

        root = new StackPane(bgCanvas, pageLayer, soundButton);

        Scene scene = new Scene(root);
        URL cssUrl = getClass().getResource("/atm.css");
        if (cssUrl != null) scene.getStylesheets().add(cssUrl.toExternalForm());

        bgCanvas.widthProperty().bind(scene.widthProperty());
        bgCanvas.heightProperty().bind(scene.heightProperty());
        scene.widthProperty().addListener((obs, oldVal, newVal) -> W = newVal.intValue());
        scene.heightProperty().addListener((obs, oldVal, newVal) -> H = newVal.intValue());

        window.setScene(scene);
        window.setTitle("Horizon Bank™");
        window.setResizable(true);
        window.setMaximized(true);
        window.setFullScreen(true);
        window.setFullScreenExitHint("");
        window.show();

        scene.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case ESCAPE -> window.setFullScreen(!window.isFullScreen());
                case ENTER -> controller.process("Ent");
                case BACK_SPACE -> controller.process("CLR");
                default -> {
                    if (e.getText() != null && e.getText().matches("[0-9]")) {
                        controller.process(e.getText());
                    }
                }
            }
        });

        showWelcomePage(onManualLogin);
    }

    // =====================================================================
    // WELCOME / LOGIN
    // =====================================================================
    private void showWelcomePage(Runnable onManualLogin) {
        this.manualLoginCallback = onManualLogin;
        clearPageLabels();

        Text welcomeTo = new Text("Welcome to");
        welcomeTo.setFont(Font.font("Georgia", FontWeight.NORMAL, 22));
        welcomeTo.setFill(Color.web("#F5ECD7"));

        Text bankName = new Text("Horizon Bank™");
        bankName.setFont(Font.font("Georgia", FontWeight.BOLD, 56));
        bankName.setFill(Color.web("#D4A843"));
        bankName.setStyle("-fx-effect: dropshadow(gaussian,rgba(0,0,0,.75),18,.25,0,3);");

        Region sep = goldSeparator(330);

        ImageView gifView = loadGif();
        gifView.setFitWidth(150);
        gifView.setFitHeight(150);
        gifView.setPreserveRatio(true);
        startFloat(gifView);

        Text instruction = new Text("Tap your NFC card to log in");
        instruction.setFont(Font.font("Georgia", FontWeight.NORMAL, 18));
        instruction.setFill(Color.web("#F5ECD7"));
        startPulse(instruction);

        Button manualBtn = ghostButton("Login Manually");
        manualBtn.setOnAction(e -> {
            SoundPlayer.playButtonPress();
            onManualLogin.run();
        });

        Button createBtn = ghostButton("Create Account");
        createBtn.setOnAction(e -> controller.process("New Account"));

        HBox actions = new HBox(18, manualBtn, createBtn);
        actions.setAlignment(Pos.CENTER);

        VBox card = new VBox(20, welcomeTo, bankName, sep, gifView, instruction, actions);
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(660);
        card.setPadding(new Insets(42));
        card.setStyle(cardStyle(0.60, 26));

        replacePage(card, true);
    }

    public void showManualLoginAccNumber() {
        showInputPage(
                "Manual Login",
                "Enter your account number",
                "Use the keypad below, then press Ent / Continue.",
                "Account number"
        );
    }

    public void showManualLoginPassword() {
        showInputPage(
                "Manual Login",
                "Enter your PIN / password",
                "For your security, the input is hidden while you type.",
                "PIN / password"
        );
    }

    public void showWelcomeGreeting(String accountType, Runnable afterGreeting) {
        clearPageLabels();

        Text hi = new Text("Welcome");
        hi.setFont(Font.font("Georgia", FontWeight.BOLD, 46));
        hi.setFill(Color.web("#F5ECD7"));
        hi.setStyle("-fx-effect: dropshadow(gaussian,rgba(0,0,0,.7),18,.25,0,3);");

        Text acct = new Text(accountType.toUpperCase() + " ACCOUNT");
        acct.setFont(Font.font("Georgia", FontWeight.BOLD, 28));
        acct.setFill(Color.web("#D4A843"));

        Text ok = new Text("✓ Authenticated successfully");
        ok.setFont(Font.font("Georgia", FontWeight.NORMAL, 17));
        ok.setFill(Color.web("#8FD18F"));

        VBox card = new VBox(16, brandHeader(), hi, acct, ok);
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(560);
        card.setPadding(new Insets(42));
        card.setStyle(cardStyle(0.70, 24));

        replacePage(card, true);

        PauseTransition hold = new PauseTransition(Duration.millis(1500));
        hold.setOnFinished(e -> {
            FadeTransition ft = fade(card, 1, 0, 550);
            ft.setOnFinished(done -> afterGreeting.run());
            ft.play();
        });
        hold.play();
    }

    // =====================================================================
    // MAIN MENU
    // =====================================================================
    public void showATMPanel() {
        showMainMenu();
    }

    public void showMainMenu() {
        clearPageLabels();

        Label welcome = new Label("Welcome");
        welcome.setStyle("-fx-font-family:'Georgia';-fx-font-size:42px;-fx-font-weight:bold;-fx-text-fill:#F5ECD7;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,.65),12,.2,0,2);");

        Label sub = new Label("Please select an option");
        sub.setStyle("-fx-font-family:'Georgia';-fx-font-size:24px;-fx-text-fill:#D4A843;");

        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(28);
        grid.setVgap(24);

        grid.add(menuButton("Deposit", "↓", "action-btn-gold"), 0, 0);
        grid.add(menuButton("Withdraw", "↑", "action-btn-gold"), 1, 0);
        grid.add(menuButton("Balance", "▣", "action-btn-gold"), 2, 0);
        grid.add(menuButton("Transfer", "⇄", "action-btn-gold"), 0, 1);
        grid.add(menuButton("Statement", "☰", "action-btn-gold"), 1, 1);
        grid.add(menuButton("Change PIN", "🔒", "action-btn-gold"), 2, 1);

        Button logout = new Button("⏻  Logout");
        logout.setUserData("Logout");
        logout.getStyleClass().addAll("atm-button", "action-btn-red");
        logout.setPrefSize(760, 78);
        logout.setStyle(logout.getStyle() + "-fx-font-size:28px;-fx-font-family:'Georgia';-fx-font-weight:bold;");
        logout.setOnAction(this::buttonClicked);

        Button faq = faqButton();

        VBox panel = new VBox(18, brandHeader(), welcome, sub, spacer(6), grid, logout, spacer(16), faq);
        panel.setAlignment(Pos.TOP_CENTER);
        panel.setPadding(new Insets(28, 34, 22, 34));
        panel.setMaxWidth(960);
        panel.setMaxHeight(930);
        panel.setStyle(cardStyle(0.72, 20));

        replacePage(panel, true);
    }

    private Button menuButton(String label, String icon, String styleClass) {
        Button b = new Button(icon + "\n" + label);
        b.setUserData(label);
        b.getStyleClass().addAll("atm-button", styleClass, "big-menu-btn");
        b.setPrefSize(230, 150);
        b.setTextAlignment(TextAlignment.CENTER);
        b.setAlignment(Pos.CENTER);
        b.setWrapText(true);
        b.setStyle("-fx-font-family:'Georgia';-fx-font-size:24px;-fx-font-weight:bold;-fx-line-spacing:12px;");
        b.setOnAction(this::buttonClicked);
        return b;
    }

    // =====================================================================
    // ACTION / RESULT PAGES
    // =====================================================================
    public void showInputPage(String title, String instruction, String helper, String displayPrompt) {
        VBox panel = actionPanel(title, instruction, helper, displayPrompt, true);
        replacePage(panel, true);
    }

    // =====================================================================
    // QUICK-WITHDRAW SELECTION SCREEN
    // =====================================================================
    public void showWithdrawQuickOptions() {
        clearPageLabels();

        Label title       = titleLabel("Withdraw");
        Label instruction = instructionLabel("How much would you like to withdraw?");
        Label helper      = helperLabel("Choose a quick amount, or select \u2018Other Amount\u2019 to enter your own.");

        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(20);
        grid.setVgap(16);

        // Left column: £10, £20, £50   |   Right column: £100, £200, Other Amount
        grid.add(quickAmountButton("\u00a310",         "W10",    false), 0, 0);
        grid.add(quickAmountButton("\u00a3100",        "W100",   false), 1, 0);
        grid.add(quickAmountButton("\u00a320",         "W20",    false), 0, 1);
        grid.add(quickAmountButton("\u00a3200",        "W200",   false), 1, 1);
        grid.add(quickAmountButton("\u00a350",         "W50",    false), 0, 2);
        grid.add(quickAmountButton("Other Amount \u2192", "WOther", true),  1, 2);

        VBox panel = new VBox(18,
                brandHeader(), title, instruction, helper,
                spacer(6), grid, spacer(6),
                backButton(), faqButton());
        panel.setAlignment(Pos.TOP_CENTER);
        panel.setPadding(new Insets(28, 34, 22, 34));
        panel.setMaxWidth(760);
        panel.setStyle(cardStyle(0.72, 20));

        replacePage(panel, true);
    }

    private Button quickAmountButton(String label, String userData, boolean isOther) {
        Button b = new Button(label);
        b.setUserData(userData);
        b.getStyleClass().addAll("atm-button", isOther ? "action-btn-red" : "action-btn-gold");
        b.setPrefSize(280, 96);
        b.setTextAlignment(TextAlignment.CENTER);
        b.setAlignment(Pos.CENTER);
        b.setWrapText(true);
        b.setStyle("-fx-font-family:'Georgia';-fx-font-size:26px;-fx-font-weight:bold;");
        b.setOnAction(this::buttonClicked);
        return b;
    }

    public void showResultPage(String title, String instruction, String body) {
        clearPageLabels();

        Label titleLbl = titleLabel(title);
        Label instLbl = instructionLabel(instruction);

        TextArea details = new TextArea(body == null ? "" : body);
        details.setEditable(false);
        details.setWrapText(true);
        details.setPrefRowCount(8);
        details.setMaxWidth(620);
        details.setStyle("-fx-font-family:'Georgia';-fx-font-size:18px;" +
                "-fx-control-inner-background:rgba(18,8,2,.75);-fx-text-fill:#F5ECD7;" +
                "-fx-background-color:rgba(18,8,2,.75);-fx-border-color:rgba(212,168,67,.45);" +
                "-fx-border-radius:12px;-fx-background-radius:12px;");

        Button back = backButton();
        back.setPrefSize(260, 58);

        VBox panel = new VBox(18, brandHeader(), titleLbl, instLbl, details, back, faqButton());
        panel.setAlignment(Pos.CENTER);
        panel.setPadding(new Insets(28, 38, 24, 38));
        panel.setMaxWidth(760);
        panel.setStyle(cardStyle(0.72, 20));

        replacePage(panel, true);
    }

    public void showCreateAccountStep(int step, String title, String instruction, String helper, String displayPrompt) {
        clearPageLabels();

        Label header = titleLabel("Create Account");
        Label subtitle = instructionLabel("Follow the steps below to set up your account");

        VBox left = new VBox(14);
        left.setAlignment(Pos.TOP_LEFT);
        left.setPadding(new Insets(22));
        left.setPrefWidth(330);
        left.setStyle(innerPanelStyle());
        Label setup = new Label("SETUP STEPS");
        setup.setStyle("-fx-font-family:'Georgia';-fx-font-size:18px;-fx-font-weight:bold;-fx-text-fill:#D4A843;");
        left.getChildren().add(setup);
        left.getChildren().add(stepRow(1, "Account number", "Choose a new account number.", step == 1));
        left.getChildren().add(stepRow(2, "Secure PIN", "Create the PIN used for login.", step == 2));
        left.getChildren().add(stepRow(3, "Account type", "1 Student, 2 Prime, 3 Saving, 4 Standard.", step == 3));
        left.getChildren().add(stepRow(4, "Confirmation", "Your account will be saved.", step == 4));
        Region leftGrow = new Region();
        VBox.setVgrow(leftGrow, Priority.ALWAYS);
        Label safe = new Label("🛡  Your information is secure and encrypted.");
        safe.setWrapText(true);
        safe.setStyle("-fx-font-family:'Georgia';-fx-font-size:14px;-fx-text-fill:#F0DFB8;");
        left.getChildren().addAll(leftGrow, safe);

        VBox right = actionPanel("Step " + step + " of 4", title, helper, displayPrompt, false);
        right.setPrefWidth(560);
        right.setMaxWidth(560);

        HBox body = new HBox(20, left, right);
        body.setAlignment(Pos.CENTER);

        VBox panel = new VBox(14, brandHeader(), header, subtitle, body, faqButton());
        panel.setAlignment(Pos.TOP_CENTER);
        panel.setPadding(new Insets(24, 28, 20, 28));
        panel.setMaxWidth(980);
        panel.setStyle(cardStyle(0.72, 20));

        replacePage(panel, true);
    }

    private HBox stepRow(int number, String title, String desc, boolean active) {
        Label num = new Label(String.valueOf(number));
        num.setAlignment(Pos.CENTER);
        num.setMinSize(42, 42);
        num.setMaxSize(42, 42);
        num.setStyle("-fx-font-family:'Georgia';-fx-font-size:20px;-fx-font-weight:bold;" +
                "-fx-text-fill:" + (active ? "#1E0A02" : "#D4A843") + ";" +
                "-fx-background-radius:42px;-fx-border-radius:42px;" +
                "-fx-background-color:" + (active ? "#D4A843" : "rgba(107,66,38,.45)") + ";" +
                "-fx-border-color:rgba(212,168,67,.65);");

        Label t = new Label(title);
        t.setStyle("-fx-font-family:'Georgia';-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:#F5ECD7;");
        Label d = new Label(desc);
        d.setWrapText(true);
        d.setStyle("-fx-font-family:'Georgia';-fx-font-size:13px;-fx-text-fill:#D7C59D;");
        VBox copy = new VBox(3, t, d);
        HBox row = new HBox(14, num, copy);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private VBox actionPanel(String title, String instruction, String helper, String displayPrompt, boolean includeShell) {
        clearPageLabels();

        pageTitleLabel = titleLabel(title);
        pageInstructionLabel = instructionLabel(instruction);
        pageHelperLabel = helperLabel(helper);

        numpadField = new TextField();
        numpadField.setEditable(false);
        numpadField.setPromptText(displayPrompt == null ? "" : displayPrompt);
        numpadField.setId("NumpadField");
        numpadField.setPrefWidth(520);
        numpadField.setMaxWidth(520);
        numpadField.setPrefHeight(72);
        numpadField.setAlignment(Pos.CENTER_RIGHT);
        numpadField.setStyle("-fx-font-size:30px;-fx-font-family:'Georgia';-fx-font-weight:bold;");

        VBox keypad = buildNumpad();

        HBox actions = new HBox(20, backButton(), continueButton());
        actions.setAlignment(Pos.CENTER);

        VBox content = new VBox(16, pageTitleLabel, pageInstructionLabel, pageHelperLabel, numpadField, keypad, actions);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(18, 28, 20, 28));
        content.setStyle(includeShell ? "" : innerPanelStyle());

        if (!includeShell) return content;

        VBox panel = new VBox(16, brandHeader(), content, faqButton());
        panel.setAlignment(Pos.CENTER);
        panel.setPadding(new Insets(28, 34, 22, 34));
        panel.setMaxWidth(760);
        panel.setStyle(cardStyle(0.72, 20));
        return panel;
    }

    private VBox buildNumpad() {
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(12);
        grid.setVgap(12);

        String[][] keys = {
                {"1", "2", "3"},
                {"4", "5", "6"},
                {"7", "8", "9"},
                {"CLR", "0", "Ent"}
        };

        for (int r = 0; r < keys.length; r++) {
            for (int c = 0; c < keys[r].length; c++) {
                String key = keys[r][c];
                Button b = new Button(key.equals("Ent") ? "✓" : key);
                b.setUserData(key);
                b.getStyleClass().addAll("atm-button", key.equals("CLR") ? "action-btn-red" : key.equals("Ent") ? "action-btn-green" : "keypad-btn");
                b.setPrefSize(148, 70);
                b.setStyle("-fx-font-size:28px;-fx-font-family:'Georgia';-fx-font-weight:bold;");
                b.setOnAction(this::buttonClicked);
                grid.add(b, c, r);
            }
        }

        VBox box = new VBox(grid);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    // =====================================================================
    // UPDATE HOOKS CALLED BY UIModel
    // =====================================================================
    public void update(String msg, String input, String result) {
        if (numpadField != null) numpadField.setText(input == null ? "" : input);
        if (pageInstructionLabel != null && msg != null && !msg.isBlank()) {
            // Keep action pages responsive without bringing back a console.
            pageInstructionLabel.setText(msg);
        }
        if (pageHelperLabel != null && result != null && !result.isBlank() && result.length() < 140) {
            pageHelperLabel.setText(result.replace("=", "").trim());
        }
    }

    public void setSoundMuted(boolean muted) {
        if (soundButton != null) soundButton.setText(muted ? "🔇" : "🔊");
    }

    public void setQuickButtonsVisible(boolean visible) {
        // Quick-withdraw buttons were intentionally removed from the new design.
        // Method kept so existing UIModel calls remain safe.
    }

    public void setNumpadVisible(boolean show) {
        // The keypad is now embedded directly in each action page instead of being
        // an overlay at the bottom.  This method is kept for compatibility.
        if (!show && numpadField != null) {
            numpadField.clear();
        }
    }

    public void resetToWelcome(Runnable onManualLogin) {
        showWelcomePage(onManualLogin);
    }

    // =====================================================================
    // BUTTONS / PAGE HELPERS
    // =====================================================================
    private Button backButton() {
        Button back = new Button("←  Go Back");
        back.setUserData("Go Back");
        back.getStyleClass().addAll("atm-button", "action-btn-gold");
        back.setPrefSize(240, 58);
        back.setStyle("-fx-font-size:20px;-fx-font-family:'Georgia';-fx-font-weight:bold;");
        back.setOnAction(this::buttonClicked);
        return back;
    }

    private Button continueButton() {
        Button cont = new Button("✓  Continue");
        cont.setUserData("Ent");
        cont.getStyleClass().addAll("atm-button", "action-btn-red");
        cont.setPrefSize(280, 58);
        cont.setStyle("-fx-font-size:20px;-fx-font-family:'Georgia';-fx-font-weight:bold;");
        cont.setOnAction(this::buttonClicked);
        return cont;
    }

    private Button faqButton() {
        Button faq = new Button("? FAQ");
        faq.setId("faqButton");
        faq.setOnAction(e -> {
            controller.process("FAQ");
            openFAQWindow();
        });
        return faq;
    }

    private Button ghostButton(String text) {
        Button b = new Button(text);
        b.setId("manualLoginBtn");
        b.setPrefWidth(190);
        b.setPrefHeight(46);
        return b;
    }

    private Label titleLabel(String text) {
        Label l = new Label(text);
        l.setWrapText(true);
        l.setTextAlignment(TextAlignment.CENTER);
        l.setAlignment(Pos.CENTER);
        l.setStyle("-fx-font-family:'Georgia';-fx-font-size:38px;-fx-font-weight:bold;-fx-text-fill:#F5ECD7;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,.65),12,.2,0,2);");
        return l;
    }

    private Label instructionLabel(String text) {
        Label l = new Label(text);
        l.setWrapText(true);
        l.setTextAlignment(TextAlignment.CENTER);
        l.setAlignment(Pos.CENTER);
        l.setMaxWidth(660);
        l.setStyle("-fx-font-family:'Georgia';-fx-font-size:22px;-fx-text-fill:#D4A843;");
        return l;
    }

    private Label helperLabel(String text) {
        Label l = new Label(text == null ? "" : text);
        l.setWrapText(true);
        l.setTextAlignment(TextAlignment.CENTER);
        l.setAlignment(Pos.CENTER);
        l.setMaxWidth(600);
        l.setStyle("-fx-font-family:'Georgia';-fx-font-size:15px;-fx-text-fill:#F0DFB8;" +
                "-fx-opacity:.88;");
        return l;
    }

    private VBox brandHeader() {
        Label bank = new Label("HORIZON BANK™");
        bank.setId("BankNameLabel");
        Region sep = goldSeparator(260);
        VBox box = new VBox(7, bank, sep);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    private Region goldSeparator(double width) {
        Region sep = new Region();
        sep.setPrefWidth(width);
        sep.setMaxWidth(width);
        sep.setPrefHeight(2);
        sep.setStyle("-fx-background-color: linear-gradient(to right, transparent, rgba(212,168,67,.85), transparent);");
        return sep;
    }

    private Region spacer(double h) {
        Region r = new Region();
        r.setMinHeight(h);
        r.setPrefHeight(h);
        return r;
    }

    private String cardStyle(double opacity, int radius) {
        return "-fx-background-color: rgba(24, 10, 3, " + opacity + ");" +
                "-fx-background-radius:" + radius + "px;" +
                "-fx-border-color: rgba(212,168,67,.62);" +
                "-fx-border-width: 1.8px;" +
                "-fx-border-radius:" + radius + "px;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,.78),40,.12,0,8);";
    }

    private String innerPanelStyle() {
        return "-fx-background-color:rgba(16,7,2,.48);" +
                "-fx-background-radius:16px;" +
                "-fx-border-radius:16px;" +
                "-fx-border-color:rgba(212,168,67,.36);" +
                "-fx-border-width:1.3px;";
    }

    private void clearPageLabels() {
        pageTitleLabel = null;
        pageInstructionLabel = null;
        pageHelperLabel = null;
        numpadField = null;
    }

    private void buttonClicked(ActionEvent event) {
        Button b = (Button) event.getSource();
        Object data = b.getUserData();
        controller.process(data == null ? b.getText() : data.toString());
    }

    private void replacePage(Node newPage, boolean animate) {
        if (!animate) {
            pageLayer.getChildren().setAll(newPage);
            return;
        }
        Node old = pageLayer.getChildren().isEmpty() ? null : pageLayer.getChildren().get(0);
        newPage.setOpacity(0);
        pageLayer.getChildren().setAll(newPage);
        FadeTransition in = fade(newPage, 0, 1, 520);
        if (old != null) {
            FadeTransition out = fade(old, 1, 0, 260);
            out.setOnFinished(e -> in.play());
            out.play();
        } else {
            in.play();
        }
    }

    // =====================================================================
    // ANIMATION / BACKGROUND
    // =====================================================================
    private FadeTransition fade(Node node, double from, double to, int ms) {
        FadeTransition ft = new FadeTransition(Duration.millis(ms), node);
        ft.setFromValue(from);
        ft.setToValue(to);
        return ft;
    }

    private void startFloat(Node node) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(2200), node);
        tt.setFromY(0);
        tt.setToY(-12);
        tt.setAutoReverse(true);
        tt.setCycleCount(Animation.INDEFINITE);
        tt.setInterpolator(Interpolator.EASE_BOTH);
        tt.play();
    }

    private void startPulse(Node node) {
        FadeTransition ft = new FadeTransition(Duration.millis(1800), node);
        ft.setFromValue(0.55);
        ft.setToValue(1.0);
        ft.setAutoReverse(true);
        ft.setCycleCount(Animation.INDEFINITE);
        ft.play();
    }

    private void initOrbs() {
        String[] colours = {"#C8A882", "#A07850", "#D4A843", "#8B5E3C", "#E8C99A", "#6B4226"};
        for (int i = 0; i < 9; i++) {
            Orb o = new Orb();
            o.x = rng.nextDouble() * W;
            o.y = rng.nextDouble() * H;
            o.radius = 130 + rng.nextDouble() * 230;
            o.dx = (rng.nextDouble() - 0.5) * 0.36;
            o.dy = (rng.nextDouble() - 0.5) * 0.36;
            o.alpha = 0.13 + rng.nextDouble() * 0.18;
            o.colour = colours[i % colours.length];
            orbs.add(o);
        }
    }

    private void startOrbAnimation() {
        GraphicsContext gc = bgCanvas.getGraphicsContext2D();
        orbTimer = new AnimationTimer() {
            @Override public void handle(long now) {
                LinearGradient bg = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                        new Stop(0.0, Color.web("#3B2314")),
                        new Stop(0.5, Color.web("#5C3317")),
                        new Stop(1.0, Color.web("#2A1508")));
                gc.setFill(bg);
                gc.fillRect(0, 0, W, H);
                for (Orb o : orbs) {
                    RadialGradient g = new RadialGradient(0, 0,
                            o.x / Math.max(W, 1), o.y / Math.max(H, 1), o.radius / Math.max(W, H),
                            true, CycleMethod.NO_CYCLE,
                            new Stop(0.0, Color.web(o.colour, o.alpha)),
                            new Stop(1.0, Color.web(o.colour, 0.0)));
                    gc.setFill(g);
                    gc.fillOval(o.x - o.radius, o.y - o.radius, o.radius * 2, o.radius * 2);
                    o.x += o.dx;
                    o.y += o.dy;
                    if (o.x < -o.radius || o.x > W + o.radius) o.dx = -o.dx;
                    if (o.y < -o.radius || o.y > H + o.radius) o.dy = -o.dy;
                }
            }
        };
        orbTimer.start();
    }

    private static class Orb {
        double x, y, radius, dx, dy, alpha;
        String colour;
    }

    // =====================================================================
    // NFC GIF / FALLBACK ICON
    // =====================================================================
    private ImageView loadGif() {
        String[] locations = {"/Tap_card_animation.gif", "file:/mnt/user-data/uploads/Tap_card_animation.gif"};
        for (String loc : locations) {
            try {
                URL url = loc.startsWith("file:") ? new URL(loc) : getClass().getResource(loc);
                if (url != null) return new ImageView(new Image(url.toExternalForm(), true));
            } catch (Exception ignored) {}
        }
        return buildFallbackNFCIcon();
    }

    private ImageView buildFallbackNFCIcon() {
        Canvas c = new Canvas(160, 160);
        GraphicsContext gc = c.getGraphicsContext2D();
        gc.setStroke(Color.web("#D4A843"));
        gc.setLineWidth(4);
        double cx = 80, cy = 80;
        for (int r = 22; r <= 72; r += 18) {
            gc.strokeArc(cx - r, cy - r, r * 2, r * 2, -45, 90, ArcType.OPEN);
        }
        gc.setFill(Color.web("#D4A843", 0.25));
        gc.fillRoundRect(25, 65, 55, 38, 8, 8);
        gc.setStroke(Color.web("#D4A843"));
        gc.setLineWidth(2);
        gc.strokeRoundRect(25, 65, 55, 38, 8, 8);
        javafx.scene.image.WritableImage wi = new javafx.scene.image.WritableImage(160, 160);
        c.snapshot(null, wi);
        return new ImageView(wi);
    }

    // =====================================================================
    // FAQ
    // =====================================================================
    private boolean faqOverlayVisible = false;

    private void openFAQWindow() {
        if (faqOverlayVisible) return;
        faqOverlayVisible = true;

        // ── Dark backdrop covering the whole screen ──────────────────────
        Region backdrop = new Region();
        backdrop.setStyle("-fx-background-color:rgba(6,2,0,0.82);");
        backdrop.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        // ── Brand header ─────────────────────────────────────────────────
        Label bankLbl = new Label("HORIZON BANK\u2122");
        bankLbl.setStyle("-fx-font-family:'Georgia';-fx-font-size:13px;-fx-font-weight:bold;" +
                "-fx-text-fill:#D4A843;-fx-letter-spacing:2px;");

        Label titleLbl = new Label("Help & FAQ");
        titleLbl.setStyle("-fx-font-family:'Georgia';-fx-font-size:28px;-fx-font-weight:bold;" +
                "-fx-text-fill:#F5ECD7;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,.7),10,.2,0,2);");

        Region sep = new Region();
        sep.setPrefHeight(2);
        sep.setPrefWidth(320);
        sep.setMaxWidth(320);
        sep.setStyle("-fx-background-color:linear-gradient(" +
                "to right,transparent,rgba(212,168,67,.9),transparent);");

        VBox header = new VBox(5, bankLbl, titleLbl, sep);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(20, 0, 14, 0));

        // ── Scrollable FAQ body ──────────────────────────────────────────
        Label body = new Label(FAQ_TEXT);
        body.setWrapText(true);
        body.setStyle("-fx-font-family:'Georgia';-fx-font-size:13.5px;" +
                "-fx-text-fill:#F0DFB8;-fx-line-spacing:3px;");
        body.setMaxWidth(480);

        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background:transparent;-fx-background-color:transparent;" +
                "-fx-border-color:transparent;");
        scroll.setPadding(new Insets(0, 6, 0, 6));
        VBox.setVgrow(scroll, Priority.ALWAYS);

        // ── Close button ─────────────────────────────────────────────────
        Button close = new Button("✕   Close");
        close.getStyleClass().addAll("atm-button", "action-btn-gold");
        close.setPrefSize(200, 48);
        close.setStyle("-fx-font-family:'Georgia';-fx-font-size:16px;-fx-font-weight:bold;");

        VBox card = new VBox(0, header, scroll, new Region(), close);
        VBox.setVgrow(card.getChildren().get(2), Priority.ALWAYS); // spacer before button
        ((Region) card.getChildren().get(2)).setMinHeight(14);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPadding(new Insets(0, 22, 22, 22));
        card.setMaxWidth(560);
        card.setMaxHeight(680);
        card.setMinWidth(340);
        card.setStyle(
                "-fx-background-color:rgba(18,7,1,0.97);" +
                        "-fx-background-radius:18px;" +
                        "-fx-border-color:rgba(212,168,67,.65);" +
                        "-fx-border-width:1.8px;" +
                        "-fx-border-radius:18px;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,.90),48,.14,0,10);");

        // ── Overlay = backdrop + centred card ────────────────────────────
        StackPane overlay = new StackPane(backdrop, card);
        overlay.setAlignment(Pos.CENTER);
        overlay.setPickOnBounds(true);

        // Insert above pageLayer but keep soundButton on top
        int soundIdx = root.getChildren().indexOf(soundButton);
        root.getChildren().add(soundIdx, overlay);

        // ── Fade in ──────────────────────────────────────────────────────
        overlay.setOpacity(0);
        card.setTranslateY(24);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), overlay);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        TranslateTransition slideIn = new TranslateTransition(Duration.millis(320), card);
        slideIn.setFromY(24);
        slideIn.setToY(0);
        slideIn.setInterpolator(Interpolator.EASE_OUT);

        fadeIn.play();
        slideIn.play();

        // ── Close action ─────────────────────────────────────────────────
        Runnable dismiss = () -> {
            if (!faqOverlayVisible) return;
            FadeTransition fadeOut = new FadeTransition(Duration.millis(220), overlay);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(ev -> {
                root.getChildren().remove(overlay);
                faqOverlayVisible = false;
            });
            fadeOut.play();
        };

        close.setOnAction(e -> dismiss.run());
        // Tap the dark backdrop to close
        backdrop.setOnMouseClicked(e -> dismiss.run());
    }

    /** @deprecated kept for old launch paths. */
    public void start(Stage window) {
        // The app now starts through initAndShowWelcome().
    }

    private static final String FAQ_TEXT =

            "══════════════════════════════════════════════\n" +
                    "  HORIZON BANK™  —  ATM HELP & FAQ\n" +
                    "══════════════════════════════════════════════\n\n" +

                    "─────────────────────────────────────────────\n" +
                    "  1.  LOGGING IN\n" +
                    "─────────────────────────────────────────────\n\n" +

                    "NFC Card Login\n" +
                    "  Tap your NFC-enabled card or phone against\n" +
                    "  the reader. The ATM detects your card\n" +
                    "  automatically and logs you in without\n" +
                    "  requiring you to type anything.\n\n" +

                    "Manual Login\n" +
                    "  Press 'Login Manually' on the welcome screen.\n" +
                    "  Enter your account number using the on-screen\n" +
                    "  keypad, press Ent, then enter your PIN and\n" +
                    "  press Ent again.\n\n" +

                    "Forgotten PIN\n" +
                    "  Visit a branch or contact support. There is\n" +
                    "  no PIN-reset option available at the ATM\n" +
                    "  for security reasons.\n\n" +

                    "─────────────────────────────────────────────\n" +
                    "  2.  NAVIGATING THE APP\n" +
                    "─────────────────────────────────────────────\n\n" +

                    "  • After login the Main Menu appears with\n" +
                    "    six service buttons and a Logout button.\n\n" +
                    "  • Every screen has a 'Go Back' button that\n" +
                    "    returns you safely to the previous screen\n" +
                    "    or the Main Menu.\n\n" +
                    "  • Press Esc to toggle full-screen mode.\n\n" +
                    "  • The 🔊 sound icon in the top-right corner\n" +
                    "    mutes or unmutes all ATM sounds.\n\n" +
                    "  • This FAQ panel is available on every screen\n" +
                    "    via the '? FAQ' button at the bottom.\n\n" +

                    "─────────────────────────────────────────────\n" +
                    "  3.  WITHDRAW\n" +
                    "─────────────────────────────────────────────\n\n" +

                    "Quick Withdraw\n" +
                    "  Pressing 'Withdraw' from the Main Menu\n" +
                    "  opens a quick-pick screen with preset\n" +
                    "  amounts: £10, £20, £50, £100, £200.\n" +
                    "  Tap the amount you want — it is processed\n" +
                    "  immediately, no further steps needed.\n\n" +

                    "Other Amount\n" +
                    "  Select 'Other Amount →' to open the full\n" +
                    "  keypad. Type any amount and press Ent or\n" +
                    "  'Continue' to confirm.\n\n" +

                    "Insufficient Funds\n" +
                    "  If your balance is too low the transaction\n" +
                    "  is declined and your balance is unchanged.\n\n" +

                    "Low Balance Warning\n" +
                    "  A warning is displayed on the result screen\n" +
                    "  whenever your balance falls below £50.\n\n" +

                    "─────────────────────────────────────────────\n" +
                    "  4.  DEPOSIT\n" +
                    "─────────────────────────────────────────────\n\n" +

                    "  • Select 'Deposit' from the Main Menu.\n" +
                    "  • Enter the amount using the on-screen\n" +
                    "    keypad and press Ent or 'Continue'.\n" +
                    "  • The funds are added to your account\n" +
                    "    instantly and a confirmation is shown.\n\n" +

                    "─────────────────────────────────────────────\n" +
                    "  5.  BALANCE ENQUIRY\n" +
                    "─────────────────────────────────────────────\n\n" +

                    "  • Select 'Balance' from the Main Menu.\n" +
                    "  • Your current available balance is\n" +
                    "    displayed immediately — no keypad entry\n" +
                    "    is required.\n" +
                    "  • A low-balance alert appears if your\n" +
                    "    balance is under £50.\n\n" +

                    "─────────────────────────────────────────────\n" +
                    "  6.  TRANSFER\n" +
                    "─────────────────────────────────────────────\n\n" +

                    "  • Select 'Transfer' from the Main Menu.\n" +
                    "  • Step 1 — enter the destination account\n" +
                    "    number and press Ent.\n" +
                    "  • Step 2 — enter the amount to send\n" +
                    "    and press Ent.\n" +
                    "  • You cannot transfer to your own account.\n" +
                    "  • The destination account must exist;\n" +
                    "    otherwise the transfer is rejected.\n" +
                    "  • Transfers are instant and cannot be\n" +
                    "    reversed at the ATM.\n\n" +

                    "─────────────────────────────────────────────\n" +
                    "  7.  MINI STATEMENT\n" +
                    "─────────────────────────────────────────────\n\n" +

                    "  • Select 'Statement' from the Main Menu.\n" +
                    "  • Shows your most recent transactions\n" +
                    "    including deposits, withdrawals and\n" +
                    "    transfers with date and amount.\n" +
                    "  • The statement is read-only and cannot\n" +
                    "    be printed from this terminal.\n\n" +

                    "─────────────────────────────────────────────\n" +
                    "  8.  CHANGE PIN\n" +
                    "─────────────────────────────────────────────\n\n" +

                    "  • Select 'Change PIN' from the Main Menu.\n" +
                    "  • Step 1 — enter your current PIN.\n" +
                    "  • Step 2 — enter the new PIN you want.\n" +
                    "  • Choose a PIN that is hard to guess and\n" +
                    "    do not share it with anyone.\n" +
                    "  • The change takes effect immediately.\n\n" +

                    "─────────────────────────────────────────────\n" +
                    "  9.  CREATING A NEW ACCOUNT\n" +
                    "─────────────────────────────────────────────\n\n" +

                    "  • On the welcome / login screen press\n" +
                    "    'Create New Account'.\n" +
                    "  • Step 1 — choose an account number.\n" +
                    "  • Step 2 — set a secure PIN.\n" +
                    "  • Step 3 — select an account type:\n\n" +
                    "      1  Student  — basic account for\n" +
                    "                    students in education.\n" +
                    "      2  Prime    — premium account with\n" +
                    "                    enhanced features.\n" +
                    "      3  Saving   — dedicated savings\n" +
                    "                    account.\n" +
                    "      4  Standard — everyday current\n" +
                    "                    account.\n\n" +
                    "  • Step 4 — confirmation. Your account is\n" +
                    "    created and saved to the database.\n\n" +

                    "─────────────────────────────────────────────\n" +
                    "  10.  NFC CARD / ANDROID COMPANION APP\n" +
                    "─────────────────────────────────────────────\n\n" +

                    "  • The Android companion app (NFC Listener)\n" +
                    "    reads your card's UID and sends it to\n" +
                    "    this ATM over a local Wi-Fi connection.\n\n" +
                    "  • Both devices must be on the same network,\n" +
                    "    or ADB reverse-tunnel must be active\n" +
                    "    (set up automatically at startup).\n\n" +
                    "  • Tap your physical NFC card to the back\n" +
                    "    of the phone running the companion app;\n" +
                    "    the ATM logs in within seconds.\n\n" +
                    "  • Each card UID is linked to exactly one\n" +
                    "    bank account in the database.\n\n" +

                    "─────────────────────────────────────────────\n" +
                    "  11.  DATABASE & SQL INSPECTOR\n" +
                    "─────────────────────────────────────────────\n\n" +

                    "  • The ATM connects to a remote MySQL\n" +
                    "    database hosted at Brighton Domains.\n" +
                    "  • All account data, balances and\n" +
                    "    transactions are stored there.\n\n" +
                    "  • A built-in SQL Inspector window opens\n" +
                    "    alongside the ATM (developer tool).\n" +
                    "  • It shows the live database schema and\n" +
                    "    lets you run any SQL query directly.\n" +
                    "  • The inspector floats above the ATM\n" +
                    "    window and can be moved freely.\n\n" +

                    "─────────────────────────────────────────────\n" +
                    "  12.  THE ON-SCREEN KEYPAD\n" +
                    "─────────────────────────────────────────────\n\n" +

                    "  • Tap digit buttons or use your keyboard\n" +
                    "    number keys to enter values.\n" +
                    "  • CLR / Backspace — deletes all input.\n" +
                    "  • ✓ (green button) / Enter key / 'Continue'\n" +
                    "    button — confirms your entry.\n" +
                    "  • Input is hidden (shown as dots) when\n" +
                    "    entering your PIN for security.\n\n" +

                    "─────────────────────────────────────────────\n" +
                    "  13.  SECURITY & SAFETY TIPS\n" +
                    "─────────────────────────────────────────────\n\n" +

                    "  • Always log out when you have finished\n" +
                    "    using the ATM.\n" +
                    "  • Never share your PIN with anyone.\n" +
                    "  • Cover the screen when entering your\n" +
                    "    PIN in a public setting.\n" +
                    "  • All connections to the database use\n" +
                    "    SSL encryption.\n" +
                    "  • Session data is cleared from memory\n" +
                    "    as soon as you log out.\n\n" +

                    "─────────────────────────────────────────────\n" +
                    "  14.  TROUBLESHOOTING\n" +
                    "─────────────────────────────────────────────\n\n" +

                    "App won't connect to database\n" +
                    "  Check your internet connection. The ATM\n" +
                    "  requires access to the remote MySQL server.\n\n" +

                    "NFC tap not detected\n" +
                    "  Ensure the Android companion app is open\n" +
                    "  and the ADB tunnel is running. Try tapping\n" +
                    "  the card again slowly.\n\n" +

                    "SQL Inspector not visible\n" +
                    "  It may be behind the ATM window. Use\n" +
                    "  Alt+Tab or check the taskbar. The inspector\n" +
                    "  always stays on top once focused.\n\n" +

                    "Transaction declined unexpectedly\n" +
                    "  Check your balance first. If funds are\n" +
                    "  sufficient, the database connection may\n" +
                    "  have timed out — try again.\n\n" +

                    "══════════════════════════════════════════════\n" +
                    "  Horizon Bank™ ATM  |  University Project\n" +
                    "  Built with Java 21 + JavaFX + MySQL\n" +
                    "══════════════════════════════════════════════\n";
}