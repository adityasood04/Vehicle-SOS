package com.example.carsos;

public class Data {
    String pitch;
    String roll;

    public Data(String pitch, String roll) {
        this.pitch = pitch;
        this.roll = roll;
    }

    public String getPitch() {
        return pitch;
    }

    public void setPitch(String pitch) {
        this.pitch = pitch;
    }

    public String getRoll() {
        return roll;
    }

    public void setRoll(String roll) {
        this.roll = roll;
    }

    public Data() {
    }
}
