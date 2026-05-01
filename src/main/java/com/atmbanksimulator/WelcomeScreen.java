package com.atmbanksimulator;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * WelcomeScreen — Full-screen animated splash/login screen for Horizon Bank ATM.
 *
 * Sequence:
 *  1. Screen appears with animated coffee-warm gradient background (slow orb drift).
 *  2. "Welcome to Horizon Bank™" fades in, holds for 2.5s, then fades out.
 *  3. NFC instruction text + animated GIF fade in at centre.
 *  4. "Login Manually" button appears at lower-right corner.
 *
 * The manual login button calls onManualLogin.run() so the ATM view can
 * transition to its normal card-number + password flow.
 */
public class WelcomeScreen {

    // -----------------------------------------------------------------------
    // Dimensions — full-screen feel
    // -----------------------------------------------------------------------
    private static final int W = 900;
    private static final int H = 650;

    // -----------------------------------------------------------------------
    // Warm coffee palette
    // -----------------------------------------------------------------------
    private static final Color COFFEE_LIGHT  = Color.web("#C8A882"); // latte
    private static final Color COFFEE_MID    = Color.web("#A07850"); // cappuccino
    private static final Color COFFEE_DARK   = Color.web("#6B4226"); // espresso
    private static final Color CREAM         = Color.web("#F5ECD7"); // cream
    private static final Color GOLD_ACCENT   = Color.web("#D4A843"); // honey gold

    private final Stage  stage;
    private final Runnable onManualLogin;
    private final Runnable onNFCLogin;     // called if NFC logic needs to resume after splash

    // UI nodes that need opacity control
    private final Text    welcomeTitle    = new Text("Welcome to");
    private final Text    bankName        = new Text("Horizon Bank™");
    private final Text    instructionText = new Text("Place your card on the\ncard reader to access your account");
    private ImageView     gifView;

    // Background canvas for animated orbs
    private Canvas        bgCanvas;
    private AnimationTimer orbTimer;

    // Animated orb state
    private final List<Orb> orbs = new ArrayList<>();
    private final Random rng = new Random(42);

    public WelcomeScreen(Stage stage, Runnable onManualLogin) {
        this.stage         = stage;
        this.onManualLogin = onManualLogin;
        this.onNFCLogin    = null;
    }

    // -----------------------------------------------------------------------
    // show() — builds the scene and starts animations
    // -----------------------------------------------------------------------
    public void show() {
        // Root stack: background canvas on bottom, UI layers on top
        StackPane root = new StackPane();
        root.setPrefSize(W, H);

        // --- Animated background canvas ---
        bgCanvas = new Canvas(W, H);
        initOrbs();
        startOrbAnimation();

        // --- Top title layer (Welcome to Horizon Bank™) ---
        VBox titleBox = buildTitleBox();

        // --- NFC prompt layer (instruction + GIF) ---
        VBox nfcBox = buildNFCBox();
        nfcBox.setOpacity(0);

        // --- Manual login button (lower right) ---
        Button loginBtn = buildManualLoginButton();
        loginBtn.setOpacity(0);

        StackPane.setAlignment(loginBtn, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(loginBtn, new Insets(0, 40, 35, 0));

        root.getChildren().addAll(bgCanvas, titleBox, nfcBox, loginBtn);

        Scene scene = new Scene(root, W, H);
        loadFonts(scene);

        stage.setScene(scene);
        stage.setTitle("Horizon Bank™");
        stage.setWidth(W);
        stage.setHeight(H);
        stage.setResizable(false);
        stage.show();

        // Start the animation sequence after a short warm-up delay
        PauseTransition warmup = new PauseTransition(Duration.millis(700));
        warmup.setOnFinished(e -> runSequence(titleBox, nfcBox, loginBtn));
        warmup.play();
    }

    // -----------------------------------------------------------------------
    // Animation sequence
    // -----------------------------------------------------------------------
    private void runSequence(VBox titleBox, VBox nfcBox, Button loginBtn) {

        // Phase 1: fade in welcome title
        FadeTransition fadeInTitle = new FadeTransition(Duration.millis(1200), titleBox);
        fadeInTitle.setFromValue(0);
        fadeInTitle.setToValue(1);

        // Phase 2: hold welcome title
        PauseTransition holdTitle = new PauseTransition(Duration.millis(2500));

        // Phase 3: fade out welcome title
        FadeTransition fadeOutTitle = new FadeTransition(Duration.millis(900), titleBox);
        fadeOutTitle.setFromValue(1);
        fadeOutTitle.setToValue(0);

        // Phase 4: fade in NFC prompt
        FadeTransition fadeInNFC = new FadeTransition(Duration.millis(1100), nfcBox);
        fadeInNFC.setFromValue(0);
        fadeInNFC.setToValue(1);

        // Phase 5: fade in manual login button (slight delay after NFC text)
        PauseTransition btnDelay = new PauseTransition(Duration.millis(400));
        FadeTransition fadeInBtn = new FadeTransition(Duration.millis(800), loginBtn);
        fadeInBtn.setFromValue(0);
        fadeInBtn.setToValue(1);

        SequentialTransition mainSeq = new SequentialTransition(
                fadeInTitle,
                holdTitle,
                fadeOutTitle,
                fadeInNFC
        );

        mainSeq.setOnFinished(e -> {
            btnDelay.setOnFinished(e2 -> fadeInBtn.play());
            btnDelay.play();
            startInstructionPulse(instructionText);
        });

        mainSeq.play();
    }

    // -----------------------------------------------------------------------
    // Gentle pulse on the instruction text to draw attention
    // -----------------------------------------------------------------------
    private void startInstructionPulse(Text node) {
        FadeTransition pulse = new FadeTransition(Duration.millis(1800), node);
        pulse.setFromValue(0.6);
        pulse.setToValue(1.0);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.setAutoReverse(true);
        pulse.play();
    }

    // -----------------------------------------------------------------------
    // Build title box
    // -----------------------------------------------------------------------
    private VBox buildTitleBox() {
        welcomeTitle.setFont(Font.font("Georgia", FontWeight.NORMAL, 26));
        welcomeTitle.setFill(CREAM);
        welcomeTitle.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.55), 12, 0, 0, 2);");

        bankName.setFont(Font.font("Georgia", FontWeight.BOLD, 56));
        bankName.setFill(GOLD_ACCENT);
        bankName.setStyle(
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.65), 18, 0.3, 0, 3);"
        );

        // Subtle shimmer line under bank name
        Region separator = new Region();
        separator.setPrefWidth(320);
        separator.setPrefHeight(2);
        separator.setStyle("-fx-background-color: linear-gradient(to right, transparent, #D4A843, transparent);");

        VBox box = new VBox(8, welcomeTitle, bankName, separator);
        box.setAlignment(Pos.CENTER);
        box.setOpacity(0);
        return box;
    }

    // -----------------------------------------------------------------------
    // Build NFC prompt box (GIF + instruction text)
    // -----------------------------------------------------------------------
    private VBox buildNFCBox() {
        // Animated GIF
        gifView = loadGif();

        // Instruction text
        instructionText.setFont(Font.font("Georgia", FontWeight.NORMAL, 20));
        instructionText.setFill(CREAM);
        instructionText.setTextAlignment(TextAlignment.CENTER);
        instructionText.setStyle(
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 8, 0, 0, 1);"
        );

        VBox box = new VBox(30, gifView, instructionText);
        box.setAlignment(Pos.CENTER);
        box.setTranslateY(20); // slight downward offset so it's not dead-centre
        return box;
    }

    // -----------------------------------------------------------------------
    // Load the NFC GIF from resources (or fallback to a placeholder icon)
    // -----------------------------------------------------------------------
    private ImageView loadGif() {
        ImageView iv = null;

        // Try to load the GIF from the uploads location (copied to resources at build)
        String[] locations = {
            "/Tap_card_animation.gif",
            "file:/mnt/user-data/uploads/Tap_card_animation.gif"
        };

        for (String loc : locations) {
            try {
                URL url = loc.startsWith("file:")
                        ? new java.net.URL(loc)
                        : getClass().getResource(loc);
                if (url != null) {
                    Image img = new Image(url.toExternalForm(), true);
                    iv = new ImageView(img);
                    break;
                }
            } catch (Exception ignored) {}
        }

        // Fallback: draw a simple NFC ring icon on a canvas
        if (iv == null) {
            iv = buildFallbackNFCIcon();
        }

        iv.setFitWidth(180);
        iv.setFitHeight(180);
        iv.setPreserveRatio(true);
        iv.setSmooth(true);

        // Gentle float animation for the GIF
        TranslateTransition float1 = new TranslateTransition(Duration.millis(2200), iv);
        float1.setFromY(0);
        float1.setToY(-12);
        float1.setAutoReverse(true);
        float1.setCycleCount(Animation.INDEFINITE);
        float1.setInterpolator(Interpolator.EASE_BOTH);
        float1.play();

        return iv;
    }

    // Fallback NFC icon drawn in-code if GIF isn't on classpath
    private ImageView buildFallbackNFCIcon() {
        Canvas c = new Canvas(160, 160);
        GraphicsContext gc = c.getGraphicsContext2D();

        // Draw concentric NFC arcs (stylised)
        gc.setStroke(GOLD_ACCENT);
        gc.setLineWidth(4);
        double cx = 80, cy = 80;
        for (int r = 20; r <= 70; r += 18) {
            gc.strokeArc(cx - r, cy - r, r * 2, r * 2, -45, 90, javafx.scene.shape.ArcType.OPEN);
        }

        // Card shape
        gc.setFill(Color.web("#D4A843", 0.25));
        gc.fillRoundRect(25, 65, 55, 38, 6, 6);
        gc.setStroke(GOLD_ACCENT);
        gc.setLineWidth(2);
        gc.strokeRoundRect(25, 65, 55, 38, 6, 6);

        // Convert canvas to ImageView (snapshot trick)
        javafx.scene.image.WritableImage wi = new javafx.scene.image.WritableImage(160, 160);
        c.snapshot(null, wi);
        return new ImageView(wi);
    }

    // -----------------------------------------------------------------------
    // Manual login button
    // -----------------------------------------------------------------------
    private Button buildManualLoginButton() {
        Button btn = new Button("Login Manually");
        btn.setStyle(
            "-fx-font-family: 'Georgia';"                    +
            "-fx-font-size: 13px;"                           +
            "-fx-text-fill: #F5ECD7;"                        +
            "-fx-background-color: rgba(107, 66, 38, 0.75);" +
            "-fx-background-radius: 6px;"                    +
            "-fx-border-color: #D4A843;"                     +
            "-fx-border-width: 1.5px;"                       +
            "-fx-border-radius: 6px;"                        +
            "-fx-padding: 8 20 8 20;"                        +
            "-fx-cursor: hand;"
        );

        btn.setOnMouseEntered(e -> btn.setStyle(
            "-fx-font-family: 'Georgia';"                    +
            "-fx-font-size: 13px;"                           +
            "-fx-text-fill: #D4A843;"                        +
            "-fx-background-color: rgba(107, 66, 38, 0.95);" +
            "-fx-background-radius: 6px;"                    +
            "-fx-border-color: #D4A843;"                     +
            "-fx-border-width: 1.5px;"                       +
            "-fx-border-radius: 6px;"                        +
            "-fx-padding: 8 20 8 20;"                        +
            "-fx-cursor: hand;"
        ));

        btn.setOnMouseExited(e -> btn.setStyle(
            "-fx-font-family: 'Georgia';"                    +
            "-fx-font-size: 13px;"                           +
            "-fx-text-fill: #F5ECD7;"                        +
            "-fx-background-color: rgba(107, 66, 38, 0.75);" +
            "-fx-background-radius: 6px;"                    +
            "-fx-border-color: #D4A843;"                     +
            "-fx-border-width: 1.5px;"                       +
            "-fx-border-radius: 6px;"                        +
            "-fx-padding: 8 20 8 20;"                        +
            "-fx-cursor: hand;"
        ));

        btn.setOnAction(e -> {
            SoundPlayer.playButtonPress();
            stopOrbAnimation();
            if (onManualLogin != null) onManualLogin.run();
        });

        return btn;
    }

    // -----------------------------------------------------------------------
    // Animated background — slowly drifting warm-toned radial orbs
    // -----------------------------------------------------------------------
    private void initOrbs() {
        // Create several soft radial light orbs
        String[] colours = {"#C8A882", "#A07850", "#D4A843", "#8B5E3C", "#E8C99A"};
        for (int i = 0; i < 6; i++) {
            Orb o = new Orb();
            o.x      = rng.nextDouble() * W;
            o.y      = rng.nextDouble() * H;
            o.radius = 120 + rng.nextDouble() * 180;
            o.dx     = (rng.nextDouble() - 0.5) * 0.4;
            o.dy     = (rng.nextDouble() - 0.5) * 0.4;
            o.alpha  = 0.18 + rng.nextDouble() * 0.20;
            o.colour = colours[i % colours.length];
            orbs.add(o);
        }
    }

    private void startOrbAnimation() {
        GraphicsContext gc = bgCanvas.getGraphicsContext2D();

        orbTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                // Base background: deep warm gradient
                LinearGradient bg = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                        new Stop(0.0, Color.web("#3B2314")),
                        new Stop(0.5, Color.web("#5C3317")),
                        new Stop(1.0, Color.web("#2A1508"))
                );
                gc.setFill(bg);
                gc.fillRect(0, 0, W, H);

                // Draw and move orbs
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

                    // Drift
                    o.x += o.dx;
                    o.y += o.dy;

                    // Bounce off walls
                    if (o.x < -o.radius || o.x > W + o.radius) o.dx = -o.dx;
                    if (o.y < -o.radius || o.y > H + o.radius) o.dy = -o.dy;
                }

                // Subtle grain overlay (drawn as semi-transparent tiny rects)
                // Skipped here to keep frame rate high — texture via CSS noise is preferred.
            }
        };
        orbTimer.start();
    }

    private void stopOrbAnimation() {
        if (orbTimer != null) orbTimer.stop();
    }

    // -----------------------------------------------------------------------
    // Load any custom fonts (falls back gracefully if unavailable)
    // -----------------------------------------------------------------------
    private void loadFonts(Scene scene) {
        // Georgia is a system font available on Windows/Mac/Linux.
        // No external font load needed.
    }

    // -----------------------------------------------------------------------
    // Simple data class for background orbs
    // -----------------------------------------------------------------------
    private static class Orb {
        double x, y, radius, dx, dy, alpha;
        String colour;
    }
}
