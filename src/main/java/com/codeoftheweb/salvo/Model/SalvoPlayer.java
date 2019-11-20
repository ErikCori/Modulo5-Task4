package com.codeoftheweb.salvo.Model;

import java.util.List;

public class SalvoPlayer {
    private int turn;
    private List<String> salvoLocations;
    //Constructor
    public SalvoPlayer(){}

    public SalvoPlayer(int turn, List<String> salvoLocations) {
        this.turn = turn;
        this.salvoLocations = salvoLocations;
    }

    //Getters
    public int getTurn() {return turn; }
    public List<String> getSalvoLocations() {return salvoLocations; }
}
