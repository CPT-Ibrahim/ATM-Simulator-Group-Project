package com.atmbanksimulator;

import javafx.application.Application;
import javafx.stage.Stage;

// 🧍Think of MVC like a human body:
// - View is the face and senses: it shows things and receives input.
// - Controller is the nerves: it carries signals to the brain and triggers actions.
// - UIModel is the brain: it holds state and logic, and queries domain services.
// - Bank / BankAccount are the real "money world" rules backed by a MySQL database.
// Together, they simulate how an ATM thinks, reacts, and handles money.

public class Main extends Application {
    public static void main(String[] args) { launch(args); }

    public void start(Stage window) {

        // Create a Bank object.
        // addBankAccount() now inserts rows into the MySQL database (using INSERT IGNORE,
        // so re-running the app will not throw a duplicate-key error).
        // You can remove these two lines once the test accounts are in the database.
        Bank bank = new Bank();
        bank.addBankAccount("10001", "11111", 100);
        bank.addBankAccount("10002", "22222", 50);

        // UIModel-View-Controller structure setup
        UIModel uiModel   = new UIModel(bank);
        View    view      = new View();
        Controller controller = new Controller();

        // Link them together so they can communicate
        view.controller   = controller;
        controller.UIModel = uiModel;
        uiModel.view      = view;

        // Start the GUI, then tell the UIModel to initialise itself
        view.start(window);
        uiModel.initialise();
    }
}
