package com.atmbanksimulator;

import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.net.URL;

// ===== 🙂 View (Eyes / Ears / Nose / Mouth / Face) =====

// The View class creates the GUI for the application.
// It does not know anything about business logic;
// it only updates the display when notified by the UIModel.
class View {
    // Adjusted window size for the new, wider layout
    int H = 700;
    int W = 460;

    Controller controller; // Reference to the Controller (part of the MVC setup)

    private Label laMsg;
    private TextField tfInput;
    private TextArea taResult;
    private Button soundBtn;
    private Button quick10;
    private Button quick20;
    private Button quick50;
    private Button quick100;
    private Button quick500; // NEW: for £500 quick withdraw
    private Button otherBtn; // NEW: for custom amount withdraw

    public void start(Stage window) {
        GridPane grid = new GridPane();
        grid.setId("Layout");

        laMsg = new Label("Welcome to Bank-ATM");
        laMsg.setId("MessageLabel");
        grid.add(laMsg, 0, 0);

        tfInput = new TextField();
        tfInput.setEditable(false);
        tfInput.setId("InputField");
        grid.add(tfInput, 0, 1);

        taResult = new TextArea();
        taResult.setEditable(false);
        taResult.setPrefRowCount(12);
        taResult.setId("ResultScreen");

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setContent(taResult);
        scrollPane.setFitToWidth(true);
        scrollPane.setId("ResultScrollPane");
        grid.add(scrollPane, 0, 2);

        // ===================================================================
        // Button Layout Logic
        // ===================================================================

        // Main container for the entire button area
        HBox buttonArea = new HBox(15);
        buttonArea.setAlignment(Pos.CENTER);

        // --- Left-side action buttons ---
        VBox leftActions = new VBox(10);
        leftActions.setAlignment(Pos.CENTER);
        leftActions.getChildren().addAll(
                createActionButton("Dep", "action-btn-blue"),
                createActionButton("W/D", "action-btn-blue"),
                createActionButton("Bal", "action-btn-blue"),
                createActionButton("ChP", "action-btn-blue")
        );

        // --- Center numeric keypad ---
        GridPane numpad = new GridPane();
        numpad.setHgap(8);
        numpad.setVgap(8);
        numpad.setAlignment(Pos.CENTER);

        // Add number buttons (row by row)
        numpad.add(createNumpadButton("7"), 0, 0);
        numpad.add(createNumpadButton("8"), 1, 0);
        numpad.add(createNumpadButton("9"), 2, 0);

        numpad.add(createNumpadButton("4"), 0, 1);
        numpad.add(createNumpadButton("5"), 1, 1);
        numpad.add(createNumpadButton("6"), 2, 1);

        numpad.add(createNumpadButton("1"), 0, 2);
        numpad.add(createNumpadButton("2"), 1, 2);
        numpad.add(createNumpadButton("3"), 2, 2);

        // Add control buttons to the numpad's bottom row (USING NEW METHOD HERE)
        numpad.add(createColoredGridButton("CLR", "action-btn-red"), 0, 3);
        numpad.add(createNumpadButton("0"), 1, 3);
        numpad.add(createColoredGridButton("Ent", "action-btn-green"), 2, 3);

        // --- Right-side action buttons ---
        VBox rightActions = new VBox(10);
        rightActions.setAlignment(Pos.CENTER);
        rightActions.getChildren().addAll(
                createActionButton("Tra", "action-btn-blue"),
                createActionButton("New", "action-btn-blue"),
                createActionButton("Smt", "action-btn-blue"),
                createActionButton("Fin", "action-btn-red")
        );

        // Add all three sections to the main button area HBox
        buttonArea.getChildren().addAll(leftActions, numpad, rightActions);

        // Add the finished button area to the main grid
        grid.add(buttonArea, 0, 3);

        // ===================================================================
        // End of Layout Logic
        // ===================================================================

        Button faqBtn = new Button("? FAQ");
        faqBtn.setId("faqButton"); // This ID will be used in CSS
        Tooltip faqTip = new Tooltip("""
                Hover tips:
                Deposit     -> enter amount + Dep
                Withdraw    -> enter amount + W/D
                Quick W/D   -> press £10 £20 £50 £100 £500 or Other
                Transfer    -> press Tra
                Balance     -> press Bal
                Statement   -> press Smt
                Password    -> press ChP
                Logout      -> press Fin
                Note: whole pounds only, no pence""");
        Tooltip.install(faqBtn, faqTip);

        faqBtn.setOnAction(e -> {
            controller.process("FAQ");
            Stage faqStage = new Stage();
            faqStage.setTitle("FAQ - Help");
            TextArea faqText = new TextArea("""
                    HOW TO USE THE ATM
                    ================================

                    Q: How do I withdraw cash?
                    A: Enter whole pound amount
                       using keypad then press W/D
                       e.g. 10, 50, 100.
                       Note: Student accounts have a
                       daily cap of £500. Prime
                       accounts can have an overdraft.

                    Q: How do I use quick withdraw?
                    A: When logged in, press the
                       green £10 £20 £50 £100 £500
                       buttons for instant withdrawal.
                       To withdraw a custom amount,
                       press 'Other', then enter the
                       amount using the keypad and
                       press W/D. These are subject
                       to your account's withdrawal rules.

                    Q: How do I check my balance?
                    A: Press Bal when logged in.
                       The balance shown is your
                       current live balance.

                    Q: How do I change my PIN?
                    A: Press ChP when logged in.
                       You'll first enter your old
                       PIN, then your new one. New
                       PINs must be at least 6
                       characters long and include
                       both letters and digits.

                    Q: How do I deposit?
                    A: Enter whole pound amount
                       using keypad then press Dep
                       e.g. 10, 50, 100.
                       Note: whole pounds only,
                       no pence accepted. Savings
                       accounts will automatically
                       add 3% interest to deposits.
                       There is a maximum balance
                       of £1,000,000,000 for all
                       account types.

                    Q: How do I logout?
                    A: Press Fin when logged in.
                       This will end your session
                       and return you to the welcome
                       screen.

                    Q: How do I create an account?
                    A: Press New on the welcome screen.
                       You'll need to choose an
                       account number (min 4 digits)
                       and a password (min 6 chars,
                       letters + digits), then
                       select an account type.

                    Q: What are the account types?
                    A: Standard: Basic account with
                       no special features.
                       Student: Daily withdrawal cap
                       of £500.
                       Prime: Allows an overdraft of
                       up to £500.
                       Saving: Earns 3% interest on
                       every deposit.

                    Q: How do I transfer money?
                    A: Press Tra, enter the
                       destination account number,
                       then the amount. Transfers
                       are subject to your own
                       account's withdrawal rules.

                    Q: How do I view my transactions?
                    A: Press Smt for a mini statement
                       showing your last 5 transactions
                       with types, amounts, and
                       balances after each.

                    Q: What is a low balance warning?
                    A: If your balance drops below £50
                       a warning is shown on screen
                       on balance checks and statements.

                    Q: What if my account is locked?
                    A: You have 3 attempts to enter
                       your PIN correctly. After 3
                       failed attempts, your account
                       will be locked for the current
                       session. You will need to
                       restart the application to try
                       logging in again, or contact
                       bank staff for assistance.

                    ================================
                    COMING SOON / ADVANCED FEATURES
                    ================================

                    Q: How are failed transactions
                       reversed?
                    A: Automated reversals for failed
                       transfers will be implemented soon.

                    Q: How do I get a receipt?
                    A: Physical receipt printing
                       is planned for future updates.

                    Q: What if the machine times out?
                    A: Automatic logout after a period
                       of inactivity is a planned feature
                       for enhanced security.

                    Q: Is audio guidance available?
                    A: Voice prompts and accessibility
                       features are currently under
                       development.

                    Q: Can text be enlarged or
                       high-contrast?
                    A: Display customization options
                       are planned for accessibility.

                    Q: Is there a simple mode for
                       older or first-time users?
                    A: A simplified user interface
                       mode is being considered.

                    Q: Accessibility support?
                    A: Comprehensive accessibility
                       features are a high priority
                       for future releases.
                    """);
            faqText.setEditable(false);
            faqText.setWrapText(true);
            faqText.setPrefSize(450, 600); // Increased size of the TextArea
            // Increased font size for the FAQ text
            faqText.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 14px;");
            faqStage.setScene(new Scene(faqText, 470, 620)); // Increased size of the FAQ window
            faqStage.show();
        });

        soundBtn = new Button("Mute Sounds");
        soundBtn.setId("soundButton");
        soundBtn.setOnAction(e -> controller.process("Mut"));

        // Quick withdrawal buttons
        quick10  = new Button("£10");
        quick20  = new Button("£20");
        quick50  = new Button("£50");
        quick100 = new Button("£100");
        quick500 = new Button("£500"); // NEW button
        otherBtn = new Button("Other"); // NEW button

        quick10.setId("quickBtn");
        quick20.setId("quickBtn");
        quick50.setId("quickBtn");
        quick100.setId("quickBtn");
        quick500.setId("quickBtn"); // NEW ID
        otherBtn.setId("quickBtn"); // NEW ID

        quick10.setOnAction(e -> controller.process("W10"));
        quick20.setOnAction(e -> controller.process("W20"));
        quick50.setOnAction(e -> controller.process("W50"));
        quick100.setOnAction(e -> controller.process("W100"));
        quick500.setOnAction(e -> controller.process("W500")); // NEW action
        otherBtn.setOnAction(e -> controller.process("Other")); // NEW action

        // Set initial visibility to false
        quick10.setVisible(false);
        quick20.setVisible(false);
        quick50.setVisible(false);
        quick100.setVisible(false);
        quick500.setVisible(false); // NEW visibility
        otherBtn.setVisible(false); // NEW visibility


        HBox quickButtonsRow1 = new HBox(10, quick10, quick20, quick50); // First row of quick buttons
        quickButtonsRow1.setAlignment(Pos.CENTER);

        HBox quickButtonsRow2 = new HBox(10, quick100, quick500, otherBtn); // Second row of quick buttons, including "Other"
        quickButtonsRow2.setAlignment(Pos.CENTER);

        // VBox to stack the two rows of quick buttons
        VBox allQuickButtons = new VBox(10, quickButtonsRow1, quickButtonsRow2); // Combine into a VBox
        allQuickButtons.setAlignment(Pos.CENTER);

        grid.add(allQuickButtons, 0, 4); // Add the combined VBox to the grid

        HBox bottomButtons = new HBox(10, faqBtn, soundBtn);
        bottomButtons.setAlignment(Pos.CENTER_RIGHT);
        grid.add(bottomButtons, 0, 5);

        Scene scene = new Scene(grid, W, H);
        URL cssUrl = this.getClass().getResource("/atm.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        } else {
            System.err.println("Error: CSS file not found: /atm.css. Make sure it is in your resources folder.");
        }

        window.setScene(scene);
        window.setTitle("ATM-Bank Simulator");
        window.show();
    }

    /**
     * Helper method to create a styled action button (for the side columns).
     */
    private Button createActionButton(String text, String styleClass) {
        Button btn = new Button(text);
        btn.setOnAction(this::buttonClicked);
        btn.getStyleClass().addAll("atm-button", styleClass);
        btn.setPrefWidth(90); // Standardize width for outer action buttons
        return btn;
    }

    /**
     * Helper method to create a styled numpad button.
     */
    private Button createNumpadButton(String text) {
        Button btn = new Button(text);
        btn.setOnAction(this::buttonClicked);
        btn.getStyleClass().addAll("atm-button", "keypad-btn");
        return btn;
    }

    /**
     * Helper method to create a coloured button that perfectly fits the inner numpad grid.
     */
    private Button createColoredGridButton(String text, String styleClass) {
        Button btn = new Button(text);
        btn.setOnAction(this::buttonClicked);
        btn.getStyleClass().addAll("atm-button", styleClass);
        // NO setPrefWidth here, so it inherits CSS size and aligns perfectly with numbers.
        return btn;
    }

    private void buttonClicked(ActionEvent event) {
        Button b = (Button) event.getSource();
        String text = b.getText();
        controller.process(text);
    }

    public void update(String msg, String tfInputMsg, String taResultMsg) {
        laMsg.setText(msg);
        tfInput.setText(tfInputMsg);
        taResult.setText(taResultMsg);
    }

    public void setSoundMuted(boolean muted) {
        if (soundBtn != null) {
            soundBtn.setText(muted ? "Unmute Sounds" : "Mute Sounds");
        }
    }

    public void setQuickButtonsVisible(boolean visible) {
        quick10.setVisible(visible);
        quick20.setVisible(visible);
        quick50.setVisible(visible);
        quick100.setVisible(visible);
        quick500.setVisible(visible); // NEW
        otherBtn.setVisible(visible); // NEW
    }
}