package com.atmbanksimulator;

import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.TilePane;
import javafx.scene.text.Text;
import javafx.stage.Stage;

// ===== 🙂 View (Eyes / Ears / Nose / Mouth / Face) =====

// The View class creates the GUI for the application.
// It does not know anything about business logic;
// it only updates the display when notified by the UIModel.
class View {
    int H = 560;;         // Height of window pixels
    int W = 520;         // Width  of window pixels

    Controller controller; // Reference to the Controller (part of the MVC setup)

    private Label laMsg;
    private TextField tfInput;
    private TextArea taResult;
    private ScrollPane scrollPane;
    private GridPane grid;
    private TilePane buttonPane;
    private Button soundBtn;
    private Button quick10;
    private Button quick20;
    private Button quick50;
    private Button quick100;

    public void start(Stage window) {
        grid = new GridPane();
        grid.setId("Layout");

        buttonPane = new TilePane();
        buttonPane.setId("Buttons");

        laMsg = new Label("Welcome to Bank-ATM");
        grid.add(laMsg, 0, 0);

        tfInput = new TextField();
        tfInput.setEditable(false);
        grid.add(tfInput, 0, 1);

        taResult = new TextArea();
        taResult.setEditable(false);
        taResult.setPrefRowCount(12);

        scrollPane = new ScrollPane();
        scrollPane.setContent(taResult);
        grid.add(scrollPane, 0, 2);

        String[][] buttonTexts = {
                {"7",   "8", "9", "", "Dep", "Tra"},
                {"4",   "5", "6", "", "W/D", "New"},
                {"1",   "2", "3", "", "Bal", "Smt"},
                {"CLR", "0", "",  "", "ChP", "Ent"},
                {"",    "",  "",  "", "Fin", ""}
        };

        for (String[] row : buttonTexts) {
            for (String text : row) {
                if (!text.isEmpty()) {
                    Button btn = new Button(text);
                    btn.setOnAction(this::buttonClicked);
                    buttonPane.getChildren().add(btn);
                } else {
                    buttonPane.getChildren().add(new Text());
                }
            }
        }

        grid.add(buttonPane, 0, 3);

        Button faqBtn = new Button("? FAQ");
        faqBtn.setId("faqButton");

        Tooltip faqTip = new Tooltip(
                "Hover tips:\n" +
                        "Deposit     -> enter amount + Dep\n" +
                        "Withdraw    -> enter amount + W/D\n" +
                        "Quick W/D   -> press £10 £20 £50 £100\n" +
                        "Transfer    -> press Tra\n" +
                        "Balance     -> press Bal\n" +
                        "Statement   -> press Smt\n" +
                        "Password    -> press ChP\n" +
                        "Logout      -> press Fin\n" +
                        "Note: whole pounds only, no pence"
        );
        Tooltip.install(faqBtn, faqTip);

        faqBtn.setOnAction(e -> {
            controller.process("FAQ");

            Stage faqStage = new Stage();
            faqStage.setTitle("FAQ - Help");

            TextArea faqText = new TextArea(
                    "HOW TO USE THE ATM\n" +
                            "================================\n\n" +
                            "Q: How do I withdraw cash?\n" +
                            "A: Enter whole pound amount\n" +
                            "   using keypad then press W/D\n" +
                            "   e.g. 10, 50, 100\n\n" +
                            "Q: How do I use quick withdraw?\n" +
                            "A: When logged in, press the\n" +
                            "   green £10 £20 £50 £100\n" +
                            "   buttons for instant withdrawal\n\n" +
                            "Q: How do I check my balance?\n" +
                            "A: Press Bal when logged in\n\n" +
                            "Q: How do I change my PIN?\n" +
                            "A: Press ChP when logged in\n\n" +
                            "Q: How do I deposit?\n" +
                            "A: Enter whole pound amount\n" +
                            "   using keypad then press Dep\n" +
                            "   e.g. 10, 50, 100\n" +
                            "   Note: whole pounds only,\n" +
                            "   no pence accepted\n\n" +
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
                            "Q: What denominations are used?\n" +
                            "A: The ATM only accepts whole\n" +
                            "   pound amounts. No pence.\n\n" +
                            "Q: How many PIN attempts allowed?\n" +
                            "A: You have 3 attempts to enter\n" +
                            "   your PIN correctly.\n" +
                            "   After 3 failed attempts your\n" +
                            "   account will be locked for\n" +
                            "   the rest of the session.\n" +
                            "   Restart the app to unlock.\n\n" +
                            "================================\n" +
                            "COMING SOON\n" +
                            "================================\n\n" +
                            "Q: How are failed transactions\n" +
                            "   reversed?\n" +
                            "A: Coming soon\n\n" +
                            "Q: How do I get a receipt?\n" +
                            "A: Coming soon\n\n" +
                            "Q: What if machine times out?\n" +
                            "A: Coming soon\n\n" +
                            "Q: Is audio guidance available?\n" +
                            "A: Coming soon\n\n" +
                            "Q: Can text be enlarged or\n" +
                            "   high-contrast?\n" +
                            "A: Coming soon\n\n" +
                            "Q: Is there a simple mode for\n" +
                            "   older or first-time users?\n" +
                            "A: Coming soon\n\n" +
                            "Q: Accessibility support?\n" +
                            "A: Coming soon"
            );

            faqText.setEditable(false);
            faqText.setWrapText(true);
            faqText.setPrefSize(400, 500);
            faqText.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12px;");

            faqStage.setScene(new Scene(faqText, 420, 520));
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

        quick10 .setId("quickBtn");
        quick20 .setId("quickBtn");
        quick50 .setId("quickBtn");
        quick100.setId("quickBtn");

        quick10 .setOnAction(e -> controller.process("W10"));
        quick20 .setOnAction(e -> controller.process("W20"));
        quick50 .setOnAction(e -> controller.process("W50"));
        quick100.setOnAction(e -> controller.process("W100"));

        quick10 .setVisible(false);
        quick20 .setVisible(false);
        quick50 .setVisible(false);
        quick100.setVisible(false);

        HBox quickButtons = new HBox(10, quick10, quick20, quick50, quick100);
        grid.add(quickButtons, 0, 4);

        HBox bottomButtons = new HBox(10, faqBtn, soundBtn);
        grid.add(bottomButtons, 0, 5);

        Scene scene = new Scene(grid, W, H);
        scene.getStylesheets().add("atm.css");
        window.setScene(scene);
        window.setTitle("ATM-Bank Simulator");
        window.show();
    }

    private void buttonClicked(ActionEvent event) {
        Button b = (Button) event.getSource();
        String text = b.getText();

        System.out.println("View::buttonClicked: label = " + text);
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
        quick10 .setVisible(visible);
        quick20 .setVisible(visible);
        quick50 .setVisible(visible);
        quick100.setVisible(visible);
    }
}
