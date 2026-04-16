package com.atmbanksimulator;

import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    public void start(Stage window) {

        // Create a Bank object.
        Bank bank = new Bank();

        // UIModel-View-Controller structure setup
        UIModel uiModel = new UIModel(bank);
        View view = new View();
        Controller controller = new Controller();

        // Link them together so they can communicate
        view.controller = controller;
        controller.UIModel = uiModel;
        uiModel.view = view;

        // Start the main ATM GUI
        view.start(window);

        // Optional DB inspector window (controlled by toggle in DBConnection)
        DBConnection.launchInspector(window);

        // Initialise the ATM state
        uiModel.initialise();
    }
}
