package com.google.zxing;

public class LuminanceThresholds {
    private int lightColor;
    private int blackColor;

    public LuminanceThresholds(int lightColor, int blackColor) {
        this.lightColor = lightColor;
        this.blackColor = blackColor;
    }

    public int getLightColor() {
        return lightColor;
    }

    public int getBlackColor() {
        return blackColor;
    }
}
