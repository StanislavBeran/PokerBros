package com.example.pokerbros;

import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
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

    @FXML private ImageView deckImage; // Zdroj pro rozdávání karet
    @FXML private ImageView flopCard1, flopCard2, flopCard3, turnCard, riverCard;
    @FXML private Label turnLabel;
    @FXML private Label player1HandLabel;
    @FXML private HBox actionPanel;
    @FXML private javafx.scene.layout.StackPane combinationsOverlay;

    private int numberOfPlayers = 2;
    private List<String> deck;

    private enum GamePhase { PRE_FLOP, FLOP, TURN, RIVER, SHOWDOWN }
    private GamePhase currentPhase;
    private int currentPlayerTurn = 0;
    private int playersActedThisPhase = 0;
    private boolean[] inGame;
    private String[][] playerCards = new String[6][2];
    private List<String> communityCards;

    @FXML
    public void initialize() {
        loadSettings();
        setupTable();
        // Malé zpoždění před první hrou, aby se scéna stihla plně vykreslit pro výpočet souřadnic
        javafx.application.Platform.runLater(this::startNewRound);
    }

    private void startNewRound() {
        createAndShuffleDeck();
        communityCards = new ArrayList<>();

        Image backImage = new Image(getClass().getResource("pack_of_cards.png").toExternalForm());
        ImageView[] tableCards = {flopCard1, flopCard2, flopCard3, turnCard, riverCard};
        for (ImageView tv : tableCards) {
            tv.setImage(backImage);
            tv.setEffect(null);
            tv.setOpacity(1.0);
            tv.setVisible(false); // Skryjeme před rozdáním
        }

        inGame = new boolean[6];
        for (int i = 0; i < numberOfPlayers; i++) {
            inGame[i] = true;
            Label status = getStatusLabel(i);
            status.setText("Waiting...");
            status.setStyle("-fx-background-color: rgba(0,0,0,0.6); -fx-background-radius: 5; -fx-padding: 3 8 3 8;");

            getCardImageView(i, 1).setEffect(null);
            getCardImageView(i, 2).setEffect(null);
            getCardImageView(i, 1).setOpacity(1.0);
            getCardImageView(i, 2).setOpacity(1.0);
            getCardImageView(i, 1).setVisible(false);
            getCardImageView(i, 2).setVisible(false);
        }

        if (player1HandLabel != null) player1HandLabel.setVisible(false);
        currentPhase = GamePhase.PRE_FLOP;
        playersActedThisPhase = 0;
        currentPlayerTurn = 0;

        dealCards();
    }

    // --- ANIMAČNÍ METODY ---

    private void animateCardDeal(ImageView targetCard, Image finalImage, int delayMillis, boolean flipAtEnd) {
        targetCard.setVisible(true);
        Image backImage = new Image(getClass().getResource("cards/cardsbackground.png").toExternalForm());
        targetCard.setImage(backImage);

        // Výpočet rozdílu souřadnic mezi balíčkem a cílovou kartou
        double startX = deckImage.localToScene(deckImage.getBoundsInLocal()).getMinX();
        double startY = deckImage.localToScene(deckImage.getBoundsInLocal()).getMinY();
        double endX = targetCard.localToScene(targetCard.getBoundsInLocal()).getMinX();
        double endY = targetCard.localToScene(targetCard.getBoundsInLocal()).getMinY();

        targetCard.setTranslateX(startX - endX);
        targetCard.setTranslateY(startY - endY);

        TranslateTransition translate = new TranslateTransition(Duration.millis(500), targetCard);
        translate.setToX(0);
        translate.setToY(0);

        RotateTransition rotate = new RotateTransition(Duration.millis(500), targetCard);
        rotate.setFromAngle(0);
        rotate.setByAngle(360);

        ParallelTransition anim = new ParallelTransition(translate, rotate);
        anim.setDelay(Duration.millis(delayMillis));

        anim.setOnFinished(e -> {
            if (flipAtEnd) {
                animateCardFlip(targetCard, finalImage);
            }
        });
        anim.play();
    }

    private void animateCardFlip(ImageView card, Image newImage) {
        ScaleTransition shrink = new ScaleTransition(Duration.millis(200), card);
        shrink.setToX(0);
        shrink.setOnFinished(e -> {
            card.setImage(newImage);
            ScaleTransition expand = new ScaleTransition(Duration.millis(200), card);
            expand.setToX(1);
            expand.play();
        });
        shrink.play();
    }

    // -----------------------

    private void dealCards() {
        int delay = 0;
        for (int i = 0; i < numberOfPlayers; i++) {
            String card1 = deck.remove(0);
            String card2 = deck.remove(0);
            playerCards[i][0] = card1;
            playerCards[i][1] = card2;

            Image img1 = new Image(getClass().getResource("cards/" + card1 + ".png").toExternalForm());
            Image img2 = new Image(getClass().getResource("cards/" + card2 + ".png").toExternalForm());

            // Hráč 1 (Vy) - karty se otočí lícem nahoru
            if (i == 0) {
                animateCardDeal(getCardImageView(0, 1), img1, delay, true);
                delay += 200;
                animateCardDeal(getCardImageView(0, 2), img2, delay, true);
            } else {
                // Roboti - karty zůstanou rubem nahoru
                animateCardDeal(getCardImageView(i, 1), null, delay, false);
                delay += 200;
                animateCardDeal(getCardImageView(i, 2), null, delay, false);
            }
            delay += 200;
        }

        PauseTransition wait = new PauseTransition(Duration.millis(delay + 500));
        wait.setOnFinished(e -> {
            updatePlayer1Hand();
            updateTurn();
        });
        wait.play();
    }

    private void nextPhase() {
        playersActedThisPhase = 0;
        currentPlayerTurn = getFirstActivePlayer();

        switch (currentPhase) {
            case PRE_FLOP:
                currentPhase = GamePhase.FLOP;
                revealCommunityCard(flopCard1, 0);
                revealCommunityCard(flopCard2, 300);
                revealCommunityCard(flopCard3, 600);
                break;
            case FLOP:
                currentPhase = GamePhase.TURN;
                revealCommunityCard(turnCard, 0);
                break;
            case TURN:
                currentPhase = GamePhase.RIVER;
                revealCommunityCard(riverCard, 0);
                break;
            case RIVER:
                currentPhase = GamePhase.SHOWDOWN;
                showdown();
                return;
        }

        for (int i = 0; i < numberOfPlayers; i++) {
            if (inGame[i]) getStatusLabel(i).setText("Waiting...");
        }

        if (currentPhase != GamePhase.SHOWDOWN) {
            PauseTransition pause = new PauseTransition(Duration.millis(1000));
            pause.setOnFinished(e -> {
                updatePlayer1Hand();
                updateTurn();
            });
            pause.play();
        }
    }

    private void revealCommunityCard(ImageView cardView, int delay) {
        String card = deck.remove(0);
        communityCards.add(card);
        Image img = new Image(getClass().getResource("cards/" + card + ".png").toExternalForm());
        animateCardDeal(cardView, img, delay, true);
    }

    private void showdown() {
        actionPanel.setVisible(false);
        List<HandEvaluator.Result> results = new ArrayList<>();

        for (int i = 0; i < numberOfPlayers; i++) {
            if (inGame[i]) {
                if (i != 0) {
                    try {
                        String c1 = playerCards[i][0];
                        String c2 = playerCards[i][1];
                        Image img1 = new Image(getClass().getResource("cards/" + c1 + ".png").toExternalForm());
                        Image img2 = new Image(getClass().getResource("cards/" + c2 + ".png").toExternalForm());
                        animateCardFlip(getCardImageView(i, 1), img1);
                        animateCardFlip(getCardImageView(i, 2), img2);
                    } catch (Exception e) {}
                }
                results.add(HandEvaluator.evaluatePlayer(i, playerCards[i][0], playerCards[i][1], communityCards));
            }
        }

        // Zpoždění pro dokončení animací otočení karet
        PauseTransition evalPause = new PauseTransition(Duration.millis(800));
        evalPause.setOnFinished(e -> evaluateWinner(results));
        evalPause.play();
    }

    private void evaluateWinner(List<HandEvaluator.Result> results) {
        if (results.size() == 1) {
            turnLabel.setText("Everyone folded! Winner is Player " + (results.get(0).playerId + 1) + " | Press SPACE for next game");
        } else {
            results.sort(Collections.reverseOrder());
            HandEvaluator.Result winner = results.get(0);

            turnLabel.setText("WINNER: Player " + (winner.playerId + 1) + " !!! | Press SPACE for next game");

            javafx.scene.effect.DropShadow goldGlow = new javafx.scene.effect.DropShadow(25, javafx.scene.paint.Color.GOLD);
            goldGlow.setSpread(0.5);

            flopCard1.setOpacity(0.5); flopCard2.setOpacity(0.5); flopCard3.setOpacity(0.5);
            turnCard.setOpacity(0.5); riverCard.setOpacity(0.5);

            for (HandEvaluator.Result res : results) {
                Label status = getStatusLabel(res.playerId);
                if (res.playerId == winner.playerId) {
                    status.setStyle("-fx-background-color: #44bd32; -fx-text-fill: white; -fx-padding: 3 8 3 8; -fx-background-radius: 5;");
                    status.setText("WINNER: " + res.handName.toUpperCase());

                    getCardImageView(res.playerId, 1).setOpacity(0.5);
                    getCardImageView(res.playerId, 2).setOpacity(0.5);

                    for (String winningCardName : res.bestCards) {
                        if (winningCardName.equals(playerCards[res.playerId][0])) {
                            getCardImageView(res.playerId, 1).setEffect(goldGlow);
                            getCardImageView(res.playerId, 1).setOpacity(1.0);
                        } else if (winningCardName.equals(playerCards[res.playerId][1])) {
                            getCardImageView(res.playerId, 2).setEffect(goldGlow);
                            getCardImageView(res.playerId, 2).setOpacity(1.0);
                        } else {
                            ImageView tableImg = getCommunityCardImageView(winningCardName);
                            if (tableImg != null) {
                                tableImg.setEffect(goldGlow);
                                tableImg.setOpacity(1.0);
                            }
                        }
                    }
                } else {
                    status.setText("Lost");
                    getCardImageView(res.playerId, 1).setOpacity(0.5);
                    getCardImageView(res.playerId, 2).setOpacity(0.5);
                }
            }
        }

        // Nastavení čekání na mezerník místo automatického restartu
        turnLabel.getScene().setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.SPACE) {
                turnLabel.getScene().setOnKeyPressed(null); // Odstranění listeneru
                startNewRound();
            }
        });
    }

    private void updatePlayer1Hand() {
        if (!inGame[0]) return;

        HandEvaluator.Result result = HandEvaluator.evaluatePlayer(0, playerCards[0][0], playerCards[0][1], communityCards);

        if (player1HandLabel != null) {
            player1HandLabel.setText(result.handName.toUpperCase());
            player1HandLabel.setVisible(true);
        }

        javafx.scene.effect.DropShadow blueGlow = new javafx.scene.effect.DropShadow(20, javafx.scene.paint.Color.DEEPSKYBLUE);
        blueGlow.setSpread(0.5);

        getCardImageView(0, 1).setEffect(null);
        getCardImageView(0, 2).setEffect(null);
        for (String c : communityCards) {
            ImageView tableImg = getCommunityCardImageView(c);
            if (tableImg != null) tableImg.setEffect(null);
        }

        for (String winningCardName : result.bestCards) {
            if (winningCardName.equals(playerCards[0][0])) {
                getCardImageView(0, 1).setEffect(blueGlow);
            } else if (winningCardName.equals(playerCards[0][1])) {
                getCardImageView(0, 2).setEffect(blueGlow);
            } else {
                ImageView tableImg = getCommunityCardImageView(winningCardName);
                if (tableImg != null) tableImg.setEffect(blueGlow);
            }
        }
    }

    private void updateTurn() {
        if (getActivePlayersCount() == 1) {
            turnLabel.setText("Everyone folded! The Winner is Player " + (getFirstActivePlayer() + 1));
            actionPanel.setVisible(false);
            showdown();
            return;
        }

        turnLabel.setText("PHASE: " + (currentPhase.toString().contains("PRE_FLOP") ? "PRE FLOP" : currentPhase) + " | Next up: Player " + (currentPlayerTurn + 1));

        if (currentPlayerTurn == 0) {
            actionPanel.setVisible(true);
            player1Status.setText("Your turn");
        } else {
            actionPanel.setVisible(false);
            playRobotTurn();
        }
    }

    private void nextTurn() {
        playersActedThisPhase++;
        if (playersActedThisPhase >= getActivePlayersCount()) {
            nextPhase();
            return;
        }

        do {
            currentPlayerTurn = (currentPlayerTurn + 1) % numberOfPlayers;
        } while (!inGame[currentPlayerTurn]);

        updateTurn();
    }

    // --- ZBYLÉ METODY ---

    private ImageView getCardImageView(int playerIndex, int cardIndex) {
        ImageView[][] allCards = {
                {p1Card1, p1Card2}, {p2Card1, p2Card2}, {p3Card1, p3Card2},
                {p4Card1, p4Card2}, {p5Card1, p5Card2}, {p6Card1, p6Card2}
        };
        return allCards[playerIndex][cardIndex - 1];
    }

    private ImageView getCommunityCardImageView(String cardName) {
        ImageView[] tableCards = {flopCard1, flopCard2, flopCard3, turnCard, riverCard};
        for (int i = 0; i < communityCards.size(); i++) {
            if (communityCards.get(i).equals(cardName)) return tableCards[i];
        }
        return null;
    }

    @FXML
    public void handleFold(ActionEvent event) {
        player1Status.setText("Fold");
        actionPanel.setVisible(false);
        inGame[0] = false;
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
        if (Math.random() < 0.1) {
            status.setText("Fold");
            inGame[currentPlayerTurn] = false;
        } else {
            status.setText("Check");
        }
        PauseTransition pause = new PauseTransition(Duration.seconds(1));
        pause.setOnFinished(e -> nextTurn());
        pause.play();
    }

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

    @FXML
    public void showCombinations(ActionEvent event) {
        if (combinationsOverlay != null) combinationsOverlay.setVisible(true);
    }

    @FXML
    public void hideCombinations(javafx.scene.input.MouseEvent event) {
        if (combinationsOverlay != null) combinationsOverlay.setVisible(false);
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