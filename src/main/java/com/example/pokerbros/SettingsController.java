package com.example.pokerbros;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.stage.Stage;

import java.io.FileWriter;
import java.io.IOException;

public class SettingsController {

    @FXML
    private Slider playerSlider;

    @FXML
    private Label playersLabel;

    @FXML
    public void initialize() {
        playerSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            playersLabel.setText("Počet hráčů: " + newValue.intValue());
        });
    }

    public void saveSettings(ActionEvent event) {
        int pocetHracu = (int) playerSlider.getValue();
        try (FileWriter writer = new FileWriter("settings.txt")) {
            writer.write("numberOfPlayers:" + pocetHracu);
            System.out.println("Nastavení úspěšně uloženo.");
            goBackToMenu(event);
        } catch (IOException e) {
            System.out.println("Chyba při ukládání do souboru.");
            e.printStackTrace();
        }
    }

    public void cancelSettings(ActionEvent event) throws IOException {
        goBackToMenu(event);
    }

    private void goBackToMenu(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("PokerMenuView.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, 1280, 740));
        stage.show();
    }
}