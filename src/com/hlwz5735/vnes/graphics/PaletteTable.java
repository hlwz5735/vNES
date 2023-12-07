/*
vNES
Copyright © 2006-2013 Open Emulation Project

This program is free software: you can redistribute it and/or modify it under
the terms of the GNU General Public License as published by the Free Software
Foundation, either version 3 of the License, or (at your option) any later
version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY
WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
PARTICULAR PURPOSE.  See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with
this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.hlwz5735.vnes.graphics;

import com.hlwz5735.vnes.common.Constants;
import com.hlwz5735.vnes.util.ColorUtil;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class PaletteTable {

    public static int[] curTable = new int[64];
    public static int[] origTable = new int[64];
    public static int[][] emphTable = new int[8][64];

    /** 当前强调色 */
    private int currentEmph = -1;

    /** 当前色相 */
    private int currentHue;

    /** 当前饱和度 */
    private int currentSaturation;

    /** 当前亮度 */
    private int currentLightness;

    /** 当前对比度 */
    private int currentContrast;


    /** 加载 NTSC 色盘 */
    public boolean loadNTSCPalette() {
        return loadPalette("palettes/ntsc.txt");
    }

    /** 加载 PAL 色盘 */
    public boolean loadPALPalette() {
        return loadPalette("palettes/pal.txt");
    }

    /** 加载色盘文件 */
    public boolean loadPalette(String paletteFilePath) {
        int r, g, b;

        try(InputStream fStr = ClassLoader.getSystemResourceAsStream(paletteFilePath)) {
            if (fStr == null) {
                // Unable to load palette.
                System.out.println("PaletteTable: Internal Palette Loaded.");
                loadDefaultPalette();
                return false;
            }

            if (paletteFilePath.toLowerCase().endsWith("pal")) {
                // Read binary palette file.
                byte[] tmp = new byte[64 * 3];

                int n = 0;
                while (n < 64) {
                    n += fStr.read(tmp, n, tmp.length - n);
                }

                int[] tmpi = new int[64 * 3];
                for (int i = 0; i < tmp.length; i++) {
                    tmpi[i] = tmp[i] & 0xFF;
                }

                for (int i = 0; i < 64; i++) {
                    r = tmpi[i * 3];
                    g = tmpi[i * 3 + 1];
                    b = tmpi[i * 3 + 2];
                    origTable[i] = r | (g << 8) | (b << 16);
                }
            } else {
                // Read text file with hex codes.
                InputStreamReader isr = new InputStreamReader(fStr);
                BufferedReader br = new BufferedReader(isr);

                String line = br.readLine();
                String hexR, hexG, hexB;
                int palIndex = 0;
                while (line != null) {
                    if (line.startsWith("#")) {
                        hexR = line.substring(1, 3);
                        hexG = line.substring(3, 5);
                        hexB = line.substring(5, 7);

                        r = Integer.decode("0x" + hexR);
                        g = Integer.decode("0x" + hexG);
                        b = Integer.decode("0x" + hexB);
                        origTable[palIndex] = r | (g << 8) | (b << 16);

                        palIndex++;

                    }
                    line = br.readLine();
                }
            }

            setEmphasis(0);
            makeTables();
            updatePalette();

            return true;
        } catch (Exception e) {
            // Unable to load palette.
            System.out.println("PaletteTable: Internal Palette Loaded.");
            loadDefaultPalette();
            return false;
        }
    }

    public void makeTables() {
        int r, g, b, col;

        // Calculate a table for each possible emphasis setting:
        for (int emph = 0; emph < 8; emph++) {
            // Determine color component factors:
            float rFactor = 1.0f, gFactor = 1.0f, bFactor = 1.0f;
            if ((emph & 1) != 0) {
                rFactor = 0.75f;
                bFactor = 0.75f;
            }
            if ((emph & 2) != 0) {
                rFactor = 0.75f;
                gFactor = 0.75f;
            }
            if ((emph & 4) != 0) {
                gFactor = 0.75f;
                bFactor = 0.75f;
            }

            // Calculate table:
            for (int i = 0; i < 64; i++) {
                col = origTable[i];
                r = (int) (ColorUtil.getRed(col) * rFactor);
                g = (int) (ColorUtil.getGreen(col) * gFactor);
                b = (int) (ColorUtil.getBlue(col) * bFactor);
                emphTable[emph][i] = ColorUtil.getRgb(r, g, b);
            }
        }
    }

    public void setEmphasis(int emph) {
        if (emph != currentEmph) {
            currentEmph = emph;
            System.arraycopy(emphTable[emph], 0, curTable, 0, 64);
            updatePalette();
        }
    }

    public int getEntry(int yiq) {
        return curTable[yiq];
    }

    public void updatePalette() {
        updatePalette(currentHue, currentSaturation, currentLightness, currentContrast);
    }

    /**
     * 修改色盘颜色
     * 对原始的色盘颜色添加增量改动
     * @param hueAdd 色相增量
     * @param saturationAdd 饱和度增量
     * @param lightnessAdd 亮度增量
     * @param contrastAdd 对比度增量
     */
    public void updatePalette(int hueAdd, int saturationAdd, int lightnessAdd, int contrastAdd) {
        int hsl, rgb;
        int h, s, l;
        int r, g, b;

        if (contrastAdd > 0) {
            contrastAdd *= 4;
        }

        for (int i = 0; i < 64; i++) {
            hsl = ColorUtil.rgbToHsl(emphTable[currentEmph][i]);
            h = ColorUtil.getHue(hsl) + hueAdd;
            s = (int) (ColorUtil.getSaturation(hsl) * (1.0 + saturationAdd / 256f));
            l = ColorUtil.getLightness(hsl);

            if (h < 0) {
                h += 255;
            }
            if (s < 0) {
                s = 0;
            }
            if (l < 0) {
                l = 0;
            }

            if (h > 255) {
                h -= 255;
            }
            if (s > 255) {
                s = 255;
            }
            if (l > 255) {
                l = 255;
            }

            rgb = ColorUtil.hslToRgb(h, s, l);

            r = ColorUtil.getRed(rgb);
            g = ColorUtil.getGreen(rgb);
            b = ColorUtil.getBlue(rgb);

            r = 128 + lightnessAdd + (int) ((r - 128) * (1.0 + contrastAdd / 256f));
            g = 128 + lightnessAdd + (int) ((g - 128) * (1.0 + contrastAdd / 256f));
            b = 128 + lightnessAdd + (int) ((b - 128) * (1.0 + contrastAdd / 256f));

            if (r < 0) {
                r = 0;
            }
            if (g < 0) {
                g = 0;
            }
            if (b < 0) {
                b = 0;
            }

            if (r > 255) {
                r = 255;
            }
            if (g > 255) {
                g = 255;
            }
            if (b > 255) {
                b = 255;
            }

            rgb = ColorUtil.getRgb(r, g, b);
            curTable[i] = rgb;
        }

        currentHue = hueAdd;
        currentSaturation = saturationAdd;
        currentLightness = lightnessAdd;
        currentContrast = contrastAdd;
    }

    /** 加载默认通用色盘 */
    public void loadDefaultPalette() {
        if (origTable == null) {
            origTable = new int[64];
        }
        System.arraycopy(Constants.defaultPaletteColors, 0, origTable, 0, 64);
        setEmphasis(0);
        makeTables();
    }

    /** 色盘复位 */
    public void reset() {
        currentEmph = 0;
        currentHue = 0;
        currentSaturation = 0;
        currentLightness = 0;

        setEmphasis(0);
        updatePalette();
    }
}
