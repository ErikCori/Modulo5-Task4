package com.codeoftheweb.salvo.Model;

import java.util.ArrayList;
import java.util.List;

public class ShipPlayer {
    private String type;
    private List<String> shipLocations = new ArrayList<>();

    public ShipPlayer(){}
    public ShipPlayer(String type, List<String> shipLocations) {
        this.type = type;
        this.shipLocations = shipLocations;
    }

    //Getters
    public String getType(){return type;}

    public List<String> getShipLocations() {return shipLocations;}
}
