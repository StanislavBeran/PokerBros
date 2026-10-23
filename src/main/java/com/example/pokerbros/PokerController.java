package com.example.pokerbros;

import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PokerController {

    @FXML private VBox player1Box, player2Box, player3Box, player4Box, player5Box, player6Box;
    @FXML private ImageView p1Card1, p1Card2, p2Card1, p2Card2, p3Card1, p3Card2;
    @FXML private ImageView p4Card1, p4Card2, p5Card1, p5Card2, p6Card1, p6Card2;
    @FXML private Label player1Status, player2Status, player3Status, player4Status, player5Status, player6Status;

    @FXML private ImageView flopCard1, flopCard2, flopCard3, turnCard, riverCard;
    @FXML private Label turnLabel;
    @FXML private HBox actionPanel;

    private int numberOfPlayers = 2;
    private List<String> deck;

    // --- Proměnné pro průběh hry ---
    private enum GamePhase { PRE_FLOP, FLOP, TURN, RIVER, SHOWDOWN }
    private GamePhase currentPhase;
    private int currentPlayerTurn = 0;
    private int playersActedThisPhase = 0; // Kolik hráčů už hrálo v aktuálním kole
    private boolean[] inGame; // Kdo nefoldnul (true = hraje, false = foldnul)

    @FXML
    public void initialize() {
        loadSettings();
        setupTable();
        startNewRound(); // Začneme první hru
    }

    private void startNewRound() {
        createAndShuffleDeck();
        hideCommunityCards();

        // Všichni přítomní hráči jsou na začátku hry aktivní
        inGame = new boolean[6];
        for (int i = 0; i < numberOfPlayers; i++) {
            inGame[i] = true;
            getStatusLabel(i).setText("Waiting...");
        }

        currentPhase = GamePhase.PRE_FLOP;
        playersActedThisPhase = 0;
        currentPlayerTurn = 0; // Začíná hráč 1 (Vy)

        dealCards();
        updateTurn();
    }

    private void hideCommunityCards() {
        // Nastaví komunitním kartám zadní stranu nebo prázdný obrázek
        String backUrl = getClass().getResource("pack_of_cards.png").toExternalForm();
        Image backImage = new Image(backUrl);

        flopCard1.setImage(backImage);
        flopCard2.setImage(backImage);
        flopCard3.setImage(backImage);
        turnCard.setImage(backImage);
        riverCard.setImage(backImage);
    }

    // ================= LOGIKA TAHŮ A FÁZÍ =================

    private void updateTurn() {
        // Pokud zbyde jen 1 hráč, vyhrál
        if (getActivePlayersCount() == 1) {
            turnLabel.setText("Všichni foldnuli! Vyhrává Hráč " + (getFirstActivePlayer() + 1));
            actionPanel.setVisible(false);
            showdown();
            return;
        }

        turnLabel.setText("Fáze: " + currentPhase + " | Na řadě: Hráč " + (currentPlayerTurn + 1));

        if (currentPlayerTurn == 0) { // Hrajete Vy
            actionPanel.setVisible(true);
            player1Status.setText("Tvůj tah");
        } else { // Hraje Robot
            actionPanel.setVisible(false);
            playRobotTurn();
        }
    }

    private void nextTurn() {
        playersActedThisPhase++;

        // Pokud už v tomto kole hráli všichni aktivní hráči, posuneme se do další fáze (Flop, Turn...)
        if (playersActedThisPhase >= getActivePlayersCount()) {
            nextPhase();
            return;
        }

        // Najdeme dalšího hráče, který nefoldnul
        do {
            currentPlayerTurn = (currentPlayerTurn + 1) % numberOfPlayers;
        } while (!inGame[currentPlayerTurn]);

        updateTurn();
    }

    private void nextPhase() {
        playersActedThisPhase = 0;
        currentPlayerTurn = getFirstActivePlayer(); // Po rozdání karet začíná vždy první aktivní hráč

        switch (currentPhase) {
            case PRE_FLOP:
                currentPhase = GamePhase.FLOP;
                revealCard(flopCard1);
                revealCard(flopCard2);
                revealCard(flopCard3);
                break;
            case FLOP:
                currentPhase = GamePhase.TURN;
                revealCard(turnCard);
                break;
            case TURN:
                currentPhase = GamePhase.RIVER;
                revealCard(riverCard);
                break;
            case RIVER:
                currentPhase = GamePhase.SHOWDOWN;
                showdown();
                return; // Konec hry, nejde se na další tah
        }

        // Reset statusů pro nové kolo sázek
        for (int i = 0; i < numberOfPlayers; i++) {
            if (inGame[i]) getStatusLabel(i).setText("Waiting...");
        }

        updateTurn();
    }

    private void revealCard(ImageView cardView) {
        String card = deck.remove(0); // Vezmeme z balíčku
        try {
            cardView.setImage(new Image(getClass().getResource(card + ".png").toExternalForm()));
        } catch (Exception e) {
            System.out.println("Chybí textury karet ve složce resources!");
        }
    }

    private void showdown() {
        turnLabel.setText("SHOWDOWN! Vyhodnocení výherce...");
        actionPanel.setVisible(false);

        // Zde v budoucnu přidáme logiku, která porovná karty a zjistí, kdo má nejlepší kombinaci.
        // Zatím po 5 vteřinách hru zrestartujeme.
        PauseTransition resetPause = new PauseTransition(Duration.seconds(5));
        resetPause.setOnFinished(e -> startNewRound());
        resetPause.play();
    }

    // ================= AKCE HRÁČŮ =================

    @FXML
    public void handleFold(ActionEvent event) {
        player1Status.setText("Fold");
        actionPanel.setVisible(false);
        inGame[0] = false; // Nastavíme, že jsme foldnuli
        nextTurn();
    }

    @FXML
    public void handleCheck(ActionEvent event) {
        player1Status.setText("Check");
        actionPanel.setVisible(false);
        nextTurn();
    }

    @FXML
    public void handleRaise(ActionEvent event) {
        player1Status.setText("Raise");
        actionPanel.setVisible(false);
        nextTurn();
    }

    private void playRobotTurn() {
        Label status = getStatusLabel(currentPlayerTurn);

        // Umělá inteligence robota - 10% šance na Fold, jinak Check
        if (Math.random() < 0.1) {
            status.setText("Fold");
            inGame[currentPlayerTurn] = false;
        } else {
            status.setText("Check");
        }

        // Pauza robota (1 vteřina), aby to neproběhlo okamžitě
        PauseTransition pause = new PauseTransition(Duration.seconds(1));
        pause.setOnFinished(e -> nextTurn());
        pause.play();
    }

    // ================= POMOCNÉ METODY =================

    private int getActivePlayersCount() {
        int count = 0;
        for (int i = 0; i < numberOfPlayers; i++) {
            if (inGame[i]) count++;
        }
        return count;
    }

    private int getFirstActivePlayer() {
        for (int i = 0; i < numberOfPlayers; i++) {
            if (inGame[i]) return i;
        }
        return 0;
    }

    private Label getStatusLabel(int playerIndex) {
        Label[] statuses = {player1Status, player2Status, player3Status, player4Status, player5Status, player6Status};
        return statuses[playerIndex];
    }

    // ... Zbytek (loadSettings, setupTable, createAndShuffleDeck, dealCards, returnToMenu)
    // doplňte z předchozího kódu níže:

    private void loadSettings() {
        try {
            File settingsFile = new File("settings.txt");
            if (settingsFile.exists()) {
                List<String> lines = Files.readAllLines(settingsFile.toPath());
                for (String line : lines) {
                    if (line.startsWith("numberOfPlayers:")) {
                        numberOfPlayers = Integer.parseInt(line.split(":")[1]);
                    }
                }
            }
        } catch (Exception e) {}
    }

    private void setupTable() {
        VBox[] playerBoxes = {player1Box, player2Box, player3Box, player4Box, player5Box, player6Box};
        for (int i = 0; i < 6; i++) {
            playerBoxes[i].setVisible(i < numberOfPlayers);
        }
    }

    private void createAndShuffleDeck() {
        deck = new ArrayList<>();
        String[] suits = {"hearts", "diamonds", "clubs", "spades"};
        String[] ranks = {"2", "3", "4", "5", "6", "7", "8", "9", "10", "jack", "queen", "king", "ace"};
        for (String suit : suits) {
            for (String rank : ranks) {
                deck.add(rank + "_of_" + suit);
            }
        }
        Collections.shuffle(deck);
    }

    private void dealCards() {
        for (int i = 0; i < numberOfPlayers; i++) {
            String card1 = deck.remove(0);
            String card2 = deck.remove(0);

            if (i == 0) {
                try {
                    p1Card1.setImage(new Image(getClass().getResource(card1 + ".png").toExternalForm()));
                    p1Card2.setImage(new Image(getClass().getResource(card2 + ".png").toExternalForm()));
                } catch (Exception e) {}
            }
        }
    }

    @FXML
    public void returnToMenu(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("PokerMenuView.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, 1280, 740));
        stage.show();
    }
}