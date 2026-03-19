package org.example.chat.games;

public class PlayerStats {

    private int wins;
    private int losses;
    private int draws;

    public void recordWin() { wins++; }
    public void recordLoss() { losses++; }
    public void recordDraw() { draws++; }

    public int getWins() { return wins; }
    public int getLosses() { return losses; }
    public int getDraws() { return draws; }
}