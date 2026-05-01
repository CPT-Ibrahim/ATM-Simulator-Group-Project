package com.atmbanksimulator;

import javafx.animation.AnimationTimer;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// ===== View (Eyes / Ears / Nose / Mouth / Face) =====
// Carries the animated warm-coffee background throughout the entire ATM session.
class View {

    // Window dimensions — wider to breathe with the new theme
    int H = 720;
    int W = 520;

    Controller controller;

    private Label     laMsg;
    private TextField tfInput;
    private TextArea  taResult;
    private Button    soundBtn;
    private Button    quick10, quick20, quick50, quick100;

    // Background animation state (shared with WelcomeScreen palette)
    private Canvas        bgCanvas;
    private AnimationTimer orbTimer;
    private final List<Orb> orbs = new ArrayList<>();
    private final Random rng = new Random(99); // different seed → slightly different drift

    // -----------------------------------------------------------------------
    // start()
    // -----------------------------------------------------------------------
    public void start(Stage window) {

        // ── Background canvas (fills the whole window) ──────────────────────
        bgCanvas = new Canvas(W, H);
        initOrbs();
        startOrbAnimation();

        // ── ATM panel: semi-transparent card floating over the background ────
        VBox atmPanel = buildATMPanel();

        // ── Root: canvas behind, panel on top ───────────────────────────────
        StackPane root = new StackPane(bgCanvas, atmPanel);
        root.setPrefSize(W, H);

        Scene scene = new Scene(root, W, H);

        // Load CSS for fine-grained control of JavaFX controls
        URL cssUrl = getClass().getResource("/atm.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        } else {
            System.err.println("[View] Warning: atm.css not found in resources.");
        }

        window.setScene(scene);
        window.setTitle("Horizon Bank™ ATM");
        window.setWidth(W);
        window.setHeight(H);
        window.setResizable(false);
        window.show();
    }

    // -----------------------------------------------------------------------
    // Build the main ATM panel (semi-transparent, rounded card)
    // -----------------------------------------------------------------------
    private VBox buildATMPanel() {

        // ── Bank name header ─────────────────────────────────────────────────
        Label bankLabel = new Label("HORIZON BANK™");
        bankLabel.setId("BankNameLabel");

        Label divider = new Label("─────────────────────────────");
        divider.setId("DividerLabel");

        // ── Message label ────────────────────────────────────────────────────
        laMsg = new Label("Welcome to Horizon Bank");
        laMsg.setId("MessageLabel");
        laMsg.setMaxWidth(Double.MAX_VALUE);

        // ── Input field ──────────────────────────────────────────────────────
        tfInput = new TextField();
        tfInput.setEditable(false);
        tfInput.setId("InputField");
        tfInput.setMaxWidth(Double.MAX_VALUE);

        // ── Result screen + scroll pane ──────────────────────────────────────
        taResult = new TextArea();
        taResult.setEditable(false);
        taResult.setPrefRowCount(10);
        taResult.setId("ResultScreen");

        ScrollPane scrollPane = new ScrollPane(taResult);
        scrollPane.setFitToWidth(true);
        scrollPane.setId("ResultScrollPane");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        // ── Button area ──────────────────────────────────────────────────────
        HBox buttonArea = buildButtonArea();

        // ── Quick withdraw row ───────────────────────────────────────────────
        HBox quickRow = buildQuickRow();

        // ── Bottom utilities (FAQ + Mute) ────────────────────────────────────
        HBox bottomRow = buildBottomRow();

        // ── Assemble panel ───────────────────────────────────────────────────
        VBox panel = new VBox(10,
                bankLabel,
                divider,
                laMsg,
                tfInput,
                scrollPane,
                buttonArea,
                quickRow,
                bottomRow
        );
        panel.setId("ATMPanel");
        panel.setPadding(new Insets(20, 22, 16, 22));
        panel.setMaxWidth(W - 40);
        panel.setMaxHeight(H - 40);
        panel.setAlignment(Pos.TOP_CENTER);

        StackPane.setAlignment(panel, Pos.CENTER);
        return panel;
    }

    // -----------------------------------------------------------------------
    // Button area (left actions | numpad | right actions)
    // -----------------------------------------------------------------------
    private HBox buildButtonArea() {
        // Left column
        VBox left = new VBox(9,
                createActionButton("Dep", "action-btn-gold"),
                createActionButton("W/D", "action-btn-gold"),
                createActionButton("Bal", "action-btn-gold"),
                createActionButton("ChP", "action-btn-cream")
        );
        left.setAlignment(Pos.CENTER);

        // Numpad
        GridPane numpad = new GridPane();
        numpad.setHgap(7);
        numpad.setVgap(7);
        numpad.setAlignment(Pos.CENTER);

        numpad.add(createNumpadButton("7"), 0, 0);
        numpad.add(createNumpadButton("8"), 1, 0);
        numpad.add(createNumpadButton("9"), 2, 0);
        numpad.add(createNumpadButton("4"), 0, 1);
        numpad.add(createNumpadButton("5"), 1, 1);
        numpad.add(createNumpadButton("6"), 2, 1);
        numpad.add(createNumpadButton("1"), 0, 2);
        numpad.add(createNumpadButton("2"), 1, 2);
        numpad.add(createNumpadButton("3"), 2, 2);
        numpad.add(createColoredGridButton("CLR", "action-btn-red"),  0, 3);
        numpad.add(createNumpadButton("0"),                            1, 3);
        numpad.add(createColoredGridButton("Ent", "action-btn-green"), 2, 3);

        // Right column
        VBox right = new VBox(9,
                createActionButton("Tra", "action-btn-gold"),
                createActionButton("New", "action-btn-gold"),
                createActionButton("Smt", "action-btn-gold"),
                createActionButton("Fin", "action-btn-red")
        );
        right.setAlignment(Pos.CENTER);

        HBox area = new HBox(14, left, numpad, right);
        area.setAlignment(Pos.CENTER);
        return area;
    }

    // -----------------------------------------------------------------------
    // Quick withdraw row
    // -----------------------------------------------------------------------
    private HBox buildQuickRow() {
        quick10  = new Button("£10");
        quick20  = new Button("£20");
        quick50  = new Button("£50");
        quick100 = new Button("£100");

        for (Button b : new Button[]{quick10, quick20, quick50, quick100}) {
            b.setId("quickBtn");
            b.setVisible(false);
        }

        quick10.setOnAction(e  -> controller.process("W10"));
        quick20.setOnAction(e  -> controller.process("W20"));
        quick50.setOnAction(e  -> controller.process("W50"));
        quick100.setOnAction(e -> controller.process("W100"));

        HBox row = new HBox(9, quick10, quick20, quick50, quick100);
        row.setAlignment(Pos.CENTER);
        return row;
    }

    // -----------------------------------------------------------------------
    // Bottom row (FAQ + Mute)
    // -----------------------------------------------------------------------
    private HBox buildBottomRow() {
        Button faqBtn = new Button("? FAQ");
        faqBtn.setId("faqButton");

        Tooltip faqTip = new Tooltip(
                "Deposit  → amount + Dep\n" +
                        "Withdraw → amount + W/D\n" +
                        "Quick W/D → £10 £20 £50 £100\n" +
                        "Transfer → Tra  |  Balance → Bal\n" +
                        "Statement → Smt  |  Logout → Fin\n" +
                        "Whole pounds only — no pence"
        );
        Tooltip.install(faqBtn, faqTip);

        faqBtn.setOnAction(e -> {
            controller.process("FAQ");
            Stage faqStage = new Stage();
            faqStage.setTitle("FAQ – Horizon Bank ATM");
            TextArea faqText = new TextArea(FAQ_TEXT);
            faqText.setEditable(false);
            faqText.setWrapText(true);
            faqText.setPrefSize(400, 500);
            faqText.setStyle(
                    "-fx-font-family: 'Georgia'; -fx-font-size: 12px;" +
                            "-fx-background-color: #2A1508; -fx-text-fill: #F5ECD7;"
            );
            faqStage.setScene(new Scene(faqText, 420, 520));
            faqStage.show();
        });

        soundBtn = new Button("Mute Sounds");
        soundBtn.setId("soundButton");
        soundBtn.setOnAction(e -> controller.process("Mut"));

        HBox row = new HBox(10, faqBtn, soundBtn);
        row.setAlignment(Pos.CENTER_RIGHT);
        return row;
    }

    // -----------------------------------------------------------------------
    // Button factory helpers
    // -----------------------------------------------------------------------
    private Button createActionButton(String text, String styleClass) {
        Button btn = new Button(text);
        btn.setOnAction(this::buttonClicked);
        btn.getStyleClass().addAll("atm-button", styleClass);
        btn.setPrefWidth(88);
        return btn;
    }

    private Button createNumpadButton(String text) {
        Button btn = new Button(text);
        btn.setOnAction(this::buttonClicked);
        btn.getStyleClass().addAll("atm-button", "keypad-btn");
        return btn;
    }

    private Button createColoredGridButton(String text, String styleClass) {
        Button btn = new Button(text);
        btn.setOnAction(this::buttonClicked);
        btn.getStyleClass().addAll("atm-button", styleClass);
        return btn;
    }

    private void buttonClicked(ActionEvent event) {
        controller.process(((Button) event.getSource()).getText());
    }

    // -----------------------------------------------------------------------
    // Public update hooks called by UIModel
    // -----------------------------------------------------------------------
    public void update(String msg, String input, String result) {
        laMsg.setText(msg);
        tfInput.setText(input);
        taResult.setText(result);
    }

    public void setSoundMuted(boolean muted) {
        if (soundBtn != null)
            soundBtn.setText(muted ? "Unmute Sounds" : "Mute Sounds");
    }

    public void setQuickButtonsVisible(boolean visible) {
        quick10.setVisible(visible);
        quick20.setVisible(visible);
        quick50.setVisible(visible);
        quick100.setVisible(visible);
    }

    // -----------------------------------------------------------------------
    // Animated background — same warm-coffee orb system as WelcomeScreen
    // -----------------------------------------------------------------------
    private void initOrbs() {
        String[] colours = {"#C8A882", "#A07850", "#D4A843", "#8B5E3C", "#E8C99A", "#6B4226"};
        for (int i = 0; i < 7; i++) {
            Orb o = new Orb();
            o.x      = rng.nextDouble() * W;
            o.y      = rng.nextDouble() * H;
            o.radius = 100 + rng.nextDouble() * 200;
            o.dx     = (rng.nextDouble() - 0.5) * 0.35;
            o.dy     = (rng.nextDouble() - 0.5) * 0.35;
            o.alpha  = 0.16 + rng.nextDouble() * 0.18;
            o.colour = colours[i % colours.length];
            orbs.add(o);
        }
    }

    private void startOrbAnimation() {
        GraphicsContext gc = bgCanvas.getGraphicsContext2D();
        orbTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                // Deep espresso base gradient
                LinearGradient bg = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                        new Stop(0.0, Color.web("#3B2314")),
                        new Stop(0.5, Color.web("#5C3317")),
                        new Stop(1.0, Color.web("#2A1508")));
                gc.setFill(bg);
                gc.fillRect(0, 0, W, H);

                for (Orb o : orbs) {
                    RadialGradient orbGrad = new RadialGradient(
                            0, 0,
                            o.x / W, o.y / H,
                            o.radius / Math.max(W, H),
                            true, CycleMethod.NO_CYCLE,
                            new Stop(0.0, Color.web(o.colour, o.alpha)),
                            new Stop(1.0, Color.web(o.colour, 0.0))
                    );
                    gc.setFill(orbGrad);
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

    // -----------------------------------------------------------------------
    // FAQ text constant
    // -----------------------------------------------------------------------
    private static final String FAQ_TEXT =
            "HOW TO USE THE ATM\n" +
                    "================================\n\n" +
                    "Q: How do I withdraw cash?\n" +
                    "A: Enter whole pound amount\n" +
                    "   using keypad then press W/D\n\n" +
                    "Q: How do I use quick withdraw?\n" +
                    "A: When logged in, press the\n" +
                    "   gold £10 £20 £50 £100 buttons\n\n" +
                    "Q: How do I check my balance?\n" +
                    "A: Press Bal when logged in\n\n" +
                    "Q: How do I change my PIN?\n" +
                    "A: Press ChP when logged in\n\n" +
                    "Q: How do I deposit?\n" +
                    "A: Enter whole pound amount\n" +
                    "   using keypad then press Dep\n\n" +
                    "Q: How do I logout?\n" +
                    "A: Press Fin when logged in\n\n" +
                    "Q: How do I create an account?\n" +
                    "A: Press New on welcome screen\n\n" +
                    "Q: How do I transfer money?\n" +
                    "A: Press Tra, enter destination\n" +
                    "   account number, then amount\n\n" +
                    "Q: How do I view my transactions?\n" +
                    "A: Press Smt for a mini statement\n" +
                    "   showing last 5 transactions\n\n" +
                    "Q: What is a low balance warning?\n" +
                    "A: If balance drops below £50\n" +
                    "   a warning is shown on screen\n\n" +
                    "Q: How many PIN attempts allowed?\n" +
                    "A: You have 3 attempts.\n" +
                    "   After 3 failures the account\n" +
                    "   is locked until app restarts.\n\n" +
                    "================================\n" +
                    "COMING SOON\n" +
                    "================================\n" +
                    "Receipts, timeout, accessibility,\n" +
                    "audio guidance, high contrast mode.";
}