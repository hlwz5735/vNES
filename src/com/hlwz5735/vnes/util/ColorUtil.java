package com.hlwz5735.vnes.util;

public class ColorUtil {

    public static int rgbToHsl(int rgb) {
        return rgbToHsl((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, (rgb) & 0xFF);
    }

    public static int rgbToHsl(int r, int g, int b) {
        float[] hsbvals = new float[3];
        hsbvals = java.awt.Color.RGBtoHSB(b, g, r, hsbvals);
        hsbvals[0] -= (float) Math.floor(hsbvals[0]);

        int ret = 0;
        ret |= (((int) (hsbvals[0] * 255d)) << 16);
        ret |= (((int) (hsbvals[1] * 255d)) << 8);
        ret |= (((int) (hsbvals[2] * 255d)));

        return ret;
    }

    public static int hslToRgb(int h, int s, int l) {
        return java.awt.Color.HSBtoRGB(h / 255.0f, s / 255.0f, l / 255.0f);
    }

    public static int hslToRgb(int hsl) {
        float h, s, l;
        h = (float) (((hsl >> 16) & 0xFF) / 255d);
        s = (float) (((hsl >> 8) & 0xFF) / 255d);
        l = (float) (((hsl) & 0xFF) / 255d);
        return java.awt.Color.HSBtoRGB(h, s, l);
    }

    public static int getHue(int hsl) {
        return (hsl >> 16) & 0xFF;
    }

    public static int getSaturation(int hsl) {
        return (hsl >> 8) & 0xFF;
    }

    public static int getLightness(int hsl) {
        return hsl & 0xFF;
    }

    public static int getRed(int rgb) {
        return (rgb >> 16) & 0xFF;
    }

    public static int getGreen(int rgb) {
        return (rgb >> 8) & 0xFF;
    }

    public static int getBlue(int rgb) {
        return rgb & 0xFF;
    }

    public static int getRgb(int r, int g, int b) {
        r &= 0xFF;
        g &= 0xFF;
        b &= 0xFF;
        return ((r << 16) | (g << 8) | (b));
    }
}
