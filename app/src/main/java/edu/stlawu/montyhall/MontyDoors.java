package edu.stlawu.montyhall;

import android.widget.ImageButton;

import java.util.Timer;
import java.util.TimerTask;

public class MontyDoors {
    // Instance Variables
    private ImageButton door;
    private String state;
    private int index;
    private boolean opened;

    // Constructor
    public MontyDoors(ImageButton door, String state, int index){
        this.door = door;
        this.state = state;
        this.index = index;
        this.opened = false;
    }

    // Getter Methods
    public ImageButton getDoor() {
        return door;
    }

    public String getState() {
        return state;
    }

    public int getIndex() {
        return this.index;
    }

    public boolean isOpened(){
        return opened;
    }

    // Setter Methods
    // Instantly open the door for resuming game
    public void setOpen(){
        this.opened = true;
        int image;
        if (this.state.equals("CAR")) {
            image = R.drawable.car;
        } else {
            image = R.drawable.goat;
        }
        this.door.setImageResource(image);
    }
}
