package com.example.pokerbros;

import java.util.*;
import java.util.stream.Collectors;

public class HandEvaluator {

    public static class Result implements Comparable<Result> {
        public int playerId;
        public int score;
        public String handName;
        public List<Integer> tieBreakers;
        public List<String> bestCards; // Nově přidáno: přesných 5 karet tvořících kombinaci

        public Result(int playerId, int score, String handName, List<Integer> tieBreakers, List<String> bestCards) {
            this.playerId = playerId;
            this.score = score;
            this.handName = handName;
            this.tieBreakers = tieBreakers;
            this.bestCards = bestCards;
        }

        @Override
        public int compareTo(Result o) {
            if (this.score != o.score) return Integer.compare(this.score, o.score);
            for (int i = 0; i < Math.min(this.tieBreakers.size(), o.tieBreakers.size()); i++) {
                if (!this.tieBreakers.get(i).equals(o.tieBreakers.get(i))) {
                    return Integer.compare(this.tieBreakers.get(i), o.tieBreakers.get(i));
                }
            }
            return 0;
        }
    }

    private static class Card {
        String name;
        int rank;
        String suit;

        Card(String name) {
            this.name = name;
            String[] parts = name.split("_of_");
            this.suit = parts[1];
            this.rank = switch (parts[0]) {
                case "jack" -> 11;
                case "queen" -> 12;
                case "king" -> 13;
                case "ace" -> 14;
                default -> Integer.parseInt(parts[0]);
            };
        }
    }

    public static Result evaluatePlayer(int playerId, String card1, String card2, List<String> community) {
        List<Card> allCards = new ArrayList<>();
        allCards.add(new Card(card1));
        allCards.add(new Card(card2));
        for (String c : community) allCards.add(new Card(c));

        allCards.sort((a, b) -> Integer.compare(b.rank, a.rank)); // Sestupně

        Map<String, List<Card>> bySuit = allCards.stream().collect(Collectors.groupingBy(c -> c.suit));
        Map<Integer, List<Card>> byRank = allCards.stream().collect(Collectors.groupingBy(c -> c.rank));

        // 1. Royal / Straight Flush / Flush
        for (List<Card> suitCards : bySuit.values()) {
            if (suitCards.size() >= 5) {
                suitCards.sort((a, b) -> Integer.compare(b.rank, a.rank));
                List<Card> straightFlush = getStraightCards(suitCards);
                if (straightFlush != null) {
                    if (straightFlush.get(0).rank == 14)
                        return new Result(playerId, 9, "Royal Flush", getRanks(straightFlush), getNames(straightFlush));
                    return new Result(playerId, 8, "Straight Flush", getRanks(straightFlush), getNames(straightFlush));
                }
                List<Card> flushCards = suitCards.subList(0, 5);
                return new Result(playerId, 5, "Flush", getRanks(flushCards), getNames(flushCards));
            }
        }

        List<List<Card>> quads = new ArrayList<>();
        List<List<Card>> trips = new ArrayList<>();
        List<List<Card>> pairs = new ArrayList<>();
        for (List<Card> rankCards : byRank.values()) {
            if (rankCards.size() == 4) quads.add(rankCards);
            else if (rankCards.size() == 3) trips.add(rankCards);
            else if (rankCards.size() == 2) pairs.add(rankCards);
        }
        quads.sort((a, b) -> Integer.compare(b.get(0).rank, a.get(0).rank));
        trips.sort((a, b) -> Integer.compare(b.get(0).rank, a.get(0).rank));
        pairs.sort((a, b) -> Integer.compare(b.get(0).rank, a.get(0).rank));

        // 2. Four of a Kind
        if (!quads.isEmpty()) {
            List<Card> core = new ArrayList<>(quads.get(0));
            List<Card> five = new ArrayList<>(core);
            five.addAll(getKickers(allCards, core, 1));
            return new Result(playerId, 7, "Four of a Kind", Arrays.asList(five.get(0).rank, five.get(4).rank), getNames(core));
        }

        // 3. Full House
        if (!trips.isEmpty() && (!pairs.isEmpty() || trips.size() > 1)) {
            List<Card> core = new ArrayList<>(trips.get(0));
            if (trips.size() > 1) core.addAll(trips.get(1).subList(0, 2));
            else core.addAll(pairs.get(0));
            return new Result(playerId, 6, "Full House", Arrays.asList(core.get(0).rank, core.get(3).rank), getNames(core));
        }

        // 4. Straight
        List<Card> straightCards = getStraightCards(allCards);
        if (straightCards != null) return new Result(playerId, 4, "Straight", getRanks(straightCards), getNames(straightCards));

        // 5. Three of a Kind
        if (!trips.isEmpty()) {
            List<Card> core = new ArrayList<>(trips.get(0));
            List<Card> five = new ArrayList<>(core);
            five.addAll(getKickers(allCards, core, 2));
            return new Result(playerId, 3, "Three of a Kind", getRanks(five), getNames(core));
        }

        // 6. Two Pair
        if (pairs.size() >= 2) {
            List<Card> core = new ArrayList<>(pairs.get(0));
            core.addAll(pairs.get(1));
            List<Card> five = new ArrayList<>(core);
            five.addAll(getKickers(allCards, core, 1));
            return new Result(playerId, 2, "Two Pair", getRanks(five), getNames(core));
        }

        // 7. One Pair
        if (!pairs.isEmpty()) {
            List<Card> core = new ArrayList<>(pairs.get(0));
            List<Card> five = new ArrayList<>(core);
            five.addAll(getKickers(allCards, core, 3));
            return new Result(playerId, 1, "One Pair", getRanks(five), getNames(core));
        }

        // 8. High Card
        List<Card> core = allCards.subList(0, 1); // Svítí jen ta 1 nejvyšší
        List<Card> five = allCards.subList(0, Math.min(5, allCards.size()));
        return new Result(playerId, 0, "High Card", getRanks(five), getNames(core));
    }

    private static List<Card> getStraightCards(List<Card> cards) {
        List<Card> unique = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();
        for (Card c : cards) {
            if (seen.add(c.rank)) unique.add(c);
        }
        if (unique.size() < 5) return null;

        for (int i = 0; i <= unique.size() - 5; i++) {
            if (unique.get(i).rank - unique.get(i + 4).rank == 4) return unique.subList(i, i + 5);
        }
        // A-5-4-3-2
        if (unique.get(0).rank == 14) {
            List<Card> lowStraight = new ArrayList<>();
            for (int r : new int[]{5, 4, 3, 2}) {
                Card found = unique.stream().filter(c -> c.rank == r).findFirst().orElse(null);
                if (found != null) lowStraight.add(found);
            }
            if (lowStraight.size() == 4) {
                lowStraight.add(0, unique.get(0)); // Pridá Eso
                return lowStraight;
            }
        }
        return null;
    }

    private static List<Card> getKickers(List<Card> all, List<Card> used, int count) {
        return all.stream().filter(c -> !used.contains(c)).limit(count).collect(Collectors.toList());
    }

    private static List<Integer> getRanks(List<Card> cards) {
        return cards.stream().map(c -> c.rank).collect(Collectors.toList());
    }

    private static List<String> getNames(List<Card> cards) {
        return cards.stream().map(c -> c.name).collect(Collectors.toList());
    }
}