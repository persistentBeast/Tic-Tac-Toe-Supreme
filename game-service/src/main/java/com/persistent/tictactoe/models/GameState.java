package com.persistent.tictactoe.models;

public class GameState {
    private String p1;
    private String p2;
    private String p1Name;
    private String p2Name;
    private String p1Status; // CONNECTED/DIS-CONNECTED
    private String p2Status;
    private long p1LastAck;
    private long p2LastAck;
    private int p1Symbol;
    private int p2Symbol;
    private String gameTurn;
    private String gameState;  // LIVE/LOADING/COMPLETED
    private String gameStatus;
    private long createdAt;
    private long updatedAt;

    public GameState() {
    }
// Getters and Setters

    // toString method for debugging
    @Override
    public String toString() {
        return "Game{" +
                "p1='" + p1 + '\'' +
                ", p2='" + p2 + '\'' +
                ", p1Name='" + p1Name + '\'' +
                ", p2Name='" + p2Name + '\'' +
                ", p1Status='" + p1Status + '\'' +
                ", p2Status='" + p2Status + '\'' +
                ", p1LastAck=" + p1LastAck +
                ", p2LastAck=" + p2LastAck +
                ", p1Symbol=" + p1Symbol +
                ", p2Symbol=" + p2Symbol +
                ", gameTurn='" + gameTurn + '\'' +
                ", gameState='" + gameState + '\'' +
                ", gameStatus='" + gameStatus + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }

    // Getters and Setters
    public String getP1() {
        return p1;
    }

    public void setP1(String p1) {
        this.p1 = p1;
    }

    public String getP2() {
        return p2;
    }

    public void setP2(String p2) {
        this.p2 = p2;
    }

    public String getP1Name() {
        return p1Name;
    }

    public void setP1Name(String p1Name) {
        this.p1Name = p1Name;
    }

    public String getP2Name() {
        return p2Name;
    }

    public void setP2Name(String p2Name) {
        this.p2Name = p2Name;
    }

    public String getP1Status() {
        return p1Status;
    }

    public void setP1Status(String p1Status) {
        this.p1Status = p1Status;
    }

    public String getP2Status() {
        return p2Status;
    }

    public void setP2Status(String p2Status) {
        this.p2Status = p2Status;
    }

    public long getP1LastAck() {
        return p1LastAck;
    }

    public void setP1LastAck(long p1LastAck) {
        this.p1LastAck = p1LastAck;
    }

    public long getP2LastAck() {
        return p2LastAck;
    }

    public void setP2LastAck(long p2LastAck) {
        this.p2LastAck = p2LastAck;
    }

    public int getP1Symbol() {
        return p1Symbol;
    }

    public void setP1Symbol(int p1Symbol) {
        this.p1Symbol = p1Symbol;
    }

    public int getP2Symbol() {
        return p2Symbol;
    }

    public void setP2Symbol(int p2Symbol) {
        this.p2Symbol = p2Symbol;
    }

    public String getGameTurn() {
        return gameTurn;
    }

    public void setGameTurn(String gameTurn) {
        this.gameTurn = gameTurn;
    }

    public String getGameState() {
        return gameState;
    }

    public void setGameState(String gameState) {
        this.gameState = gameState;
    }

    public String getGameStatus() {
        return gameStatus;
    }

    public void setGameStatus(String gameStatus) {
        this.gameStatus = gameStatus;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}

