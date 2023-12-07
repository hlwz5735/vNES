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

import com.hlwz5735.vnes.core.Nes;
import com.hlwz5735.vnes.common.Globals;
import com.hlwz5735.vnes.core.ByteBuffer;
import com.hlwz5735.vnes.core.Cpu;
import com.hlwz5735.vnes.core.HiResTimer;
import com.hlwz5735.vnes.core.Memory;
import com.hlwz5735.vnes.core.Rom;
import com.hlwz5735.vnes.gui.BufferView;
import java.util.Arrays;

public class Ppu {
    // Status flags:
    public static int STATUS_VRAMWRITE = 4;
    public static int STATUS_SLSPRITECOUNT = 5;
    public static int STATUS_SPRITE0HIT = 6;
    public static int STATUS_VBLANK = 7;


    //<editor-fold desc="模拟硬件组成">
    private Nes nes;
    private HiResTimer timer;
    private Memory ppuMem;
    private Memory sprMem;
    //</editor-fold>

    // Rendering Options:
    boolean showSpr0Hit = false;
    boolean showSoundBuffer = false;
    boolean clipTvColumn = true;
    boolean clipTvRow = false;

    // Control Flags Register 1:
    public int fNmiOnVBlank;    // NMI on VBlank. 0=disable, 1=enable
    public int fSpriteSize;     // Sprite size. 0=8x8, 1=8x16
    public int fBgPatternTable; // Background Pattern Table address. 0=0x0000,1=0x1000
    public int fSpPatternTable; // Sprite Pattern Table address. 0=0x0000,1=0x1000
    public int fAddrInc;        // PPU Address Increment. 0=1,1=32
    public int fNameTableAddress;    // Name Table Address. 0=0x2000,1=0x2400,2=0x2800,3=0x2C00

    // Control Flags Register 2:
    public int fColor;        // Background color. 0=black, 1=blue, 2=green, 4=red
    public int fSpVisibility; // Sprite visibility. 0=not displayed,1=displayed
    public int fBgVisibility; // Background visibility. 0=Not Displayed,1=displayed
    public int fSpClipping;   // Sprite clipping. 0=Sprites invisible in left 8-pixel column,1=No clipping
    public int fBgClipping;   // Background clipping. 0=BG invisible in left 8-pixel column, 1=No clipping
    public int fDispType;     // Display type. 0=color, 1=monochrome


    // VRAM I/O:
    int vramAddress;
    int vramTmpAddress;
    short vramBufferedReadValue;
    boolean firstWrite = true;        // VRAM/Scroll Hi/Lo latch
    int[] vramMirrorTable;            // Mirroring Lookup Table.

    // SPR-RAM I/O:
    short sramAddress; // 8-bit only.

    // Counters:
    int cntFv;
    int cntV;
    int cntH;
    int cntVt;
    int cntHt;

    // Registers:
    int regFv;
    int regV;
    int regH;
    int regVt;
    int regHt;
    int regFh;
    int regS;

    // VBlank extension for PAL emulation:
    int vBlankAdd = 0;
    public int curX;
    public int scanline;
    public int lastRenderedScanline;
    public int mapperIrqCounter;

    // Sprite data:
    public int[] sprX;           // X coordinate
    public int[] sprY;           // Y coordinate
    public int[] sprTile;        // Tile Index (into pattern table)
    public int[] sprCol;         // Upper two bits of color
    public boolean[] vertFlip;   // Vertical Flip
    public boolean[] horiFlip;   // Horizontal Flip
    public boolean[] bgPriority; // Background priority
    public int spr0HitX;    // Sprite #0 hit X coordinate
    public int spr0HitY;    // Sprite #0 hit Y coordinate
    boolean hitSpr0;

    // Tiles:
    public Tile[] ptTile;

    // Name table data:
    int[] nameTable1 = new int[4];
    NameTable[] nameTable;
    int currentMirroring = -1;

    // Palette data:
    int[] sprPalette = new int[16];
    int[] imgPalette = new int[16];

    // Misc:
    boolean scanlineAlreadyRendered;
    boolean requestEndFrame;
    boolean nmiOk;
    int nmiCounter;
    boolean dummyCycleToggle;

    // Vars used when updating regs/address:
    int address;
    int b1;
    int b2;

    // Variables used when rendering:
    int[] attributes = new int[32];
    int[] bgBuffer = new int[256 * 240];
    int[] pixrendered = new int[256 * 240];
    int[] spr0DummyBuffer = new int[256 * 240];
    int[] dummyPixPriTable = new int[256 * 240];
    int[] oldFrame = new int[256 * 240];
    int[] buffer;
    int[] tpix;
    boolean[] scanlineChanged = new boolean[240];
    boolean requestRenderAll = false;
    boolean validTileData;
    int att;
    Tile[] scantile = new Tile[32];
    Tile t;

    // These are temporary variables used in rendering and sound procedures.
    // Their states outside of those procedures can be ignored.
    int curNt;
    int destIndex;
    int x;
    int y;
    int sx;
    int si;
    int ei;
    int tile;
    int col;
    int baseTile;
    int tScanOffset;
    int srcY1;
    int srcY2;
    int bufferSize;
    int available;
    int scale;
    public int cycles = 0;

    public Ppu(Nes nes) {
        this.nes = nes;
    }

    public void init() {
        // Get the memory:
        ppuMem = nes.getPpuMemory();
        sprMem = nes.getSprMemory();

        updateControlReg1(0);
        updateControlReg2(0);

        // Initialize misc vars:
        scanline = 0;
        timer = nes.getManager().getTimer();

        // Create sprite arrays:
        sprX = new int[64];
        sprY = new int[64];
        sprTile = new int[64];
        sprCol = new int[64];
        vertFlip = new boolean[64];
        horiFlip = new boolean[64];
        bgPriority = new boolean[64];

        // Create pattern table tile buffers:
        if (ptTile == null) {
            ptTile = new Tile[512];
            for (int i = 0; i < 512; i++) {
                ptTile[i] = new Tile();
            }
        }

        // Create nametable buffers:
        nameTable = new NameTable[4];
        for (int i = 0; i < 4; i++) {
            nameTable[i] = new NameTable(32, 32, "Nt" + i);
        }

        // Initialize mirroring lookup table:
        vramMirrorTable = new int[0x8000];
        for (int i = 0; i < 0x8000; i++) {
            vramMirrorTable[i] = i;
        }

        lastRenderedScanline = -1;
        curX = 0;

        // Initialize old frame buffer:
        Arrays.fill(oldFrame, -1);
    }


    // Sets Nametable mirroring.
    public void setMirroring(int mirroring) {
        if (mirroring == currentMirroring) {
            return;
        }

        currentMirroring = mirroring;
        triggerRendering();

        // Remove mirroring:
        if (vramMirrorTable == null) {
            vramMirrorTable = new int[0x8000];
        }
        for (int i = 0; i < 0x8000; i++) {
            vramMirrorTable[i] = i;
        }

        // Palette mirroring:
        defineMirrorRegion(0x3f20, 0x3f00, 0x20);
        defineMirrorRegion(0x3f40, 0x3f00, 0x20);
        defineMirrorRegion(0x3f80, 0x3f00, 0x20);
        defineMirrorRegion(0x3fc0, 0x3f00, 0x20);

        // Additional mirroring:
        defineMirrorRegion(0x3000, 0x2000, 0xf00);
        defineMirrorRegion(0x4000, 0x0000, 0x4000);

        if (mirroring == Rom.HORIZONTAL_MIRRORING) {
            // Horizontal mirroring.
            nameTable1[0] = 0;
            nameTable1[1] = 0;
            nameTable1[2] = 1;
            nameTable1[3] = 1;

            defineMirrorRegion(0x2400, 0x2000, 0x400);
            defineMirrorRegion(0x2c00, 0x2800, 0x400);
        } else if (mirroring == Rom.VERTICAL_MIRRORING) {
            // Vertical mirroring.
            nameTable1[0] = 0;
            nameTable1[1] = 1;
            nameTable1[2] = 0;
            nameTable1[3] = 1;

            defineMirrorRegion(0x2800, 0x2000, 0x400);
            defineMirrorRegion(0x2c00, 0x2400, 0x400);
        } else if (mirroring == Rom.SINGLESCREEN_MIRRORING) {
            // Single Screen mirroring
            nameTable1[0] = 0;
            nameTable1[1] = 0;
            nameTable1[2] = 0;
            nameTable1[3] = 0;

            defineMirrorRegion(0x2400, 0x2000, 0x400);
            defineMirrorRegion(0x2800, 0x2000, 0x400);
            defineMirrorRegion(0x2c00, 0x2000, 0x400);
        } else if (mirroring == Rom.SINGLESCREEN_MIRRORING2) {
            nameTable1[0] = 1;
            nameTable1[1] = 1;
            nameTable1[2] = 1;
            nameTable1[3] = 1;

            defineMirrorRegion(0x2400, 0x2400, 0x400);
            defineMirrorRegion(0x2800, 0x2400, 0x400);
            defineMirrorRegion(0x2c00, 0x2400, 0x400);
        } else {
            // Assume Four-screen mirroring.

            nameTable1[0] = 0;
            nameTable1[1] = 1;
            nameTable1[2] = 2;
            nameTable1[3] = 3;
        }
    }

    // Define a mirrored area in the address lookup table.
    // Assumes the regions don't overlap.
    // The 'to' region is the region that is physically in memory.
    private void defineMirrorRegion(int fromStart, int toStart, int size) {
        for (int i = 0; i < size; i++) {
            vramMirrorTable[fromStart + i] = toStart + i;
        }
    }

    // Emulates PPU cycles
    public void emulateCycles() {
        // int n = (!requestEndFrame && curX+cycles<341 && (scanline-20 < spr0HitY || scanline-22 > spr0HitY))?cycles:1;
        for (; cycles > 0; cycles--) {
            if (scanline - 21 == spr0HitY) {
                if ((curX == spr0HitX) && (fSpVisibility == 1)) {
                    // Set sprite 0 hit flag:
                    setStatusFlag(STATUS_SPRITE0HIT, true);
                }
            }

            if (requestEndFrame) {
                nmiCounter--;
                if (nmiCounter == 0) {
                    requestEndFrame = false;
                    startVBlank();
                }
            }

            curX++;
            if (curX == 341) {
                curX = 0;
                endScanline();
            }
        }
    }

    public void startVBlank() {
        // Start VBlank period:
        // Do VBlank.
        // if (Globals.debug) {
            // Globals.println("VBlank occurs!");
        // }

        // Do NMI:
        nes.getCpu().requestIrq(Cpu.IRQ_NMI);

        // Make sure everything is rendered:
        if (lastRenderedScanline < 239) {
            renderFramePartially(nes.getManager().getScreenView().getBuffer(),
                    lastRenderedScanline + 1,
                    240 - lastRenderedScanline);
        }

        endFrame();

        // Notify image buffer:
        nes.getManager().getScreenView().imageReady(false);

        // Reset scanline counter:
        lastRenderedScanline = -1;

        startFrame();
    }

    public void endScanline() {
        if (scanline < 19 + vBlankAdd) {
            // VINT
            // do nothing.
        } else if (scanline == 19 + vBlankAdd) {
            // Dummy scanline.
            // May be variable length:
            if (dummyCycleToggle) {
                // Remove dead cycle at end of scanline,
                // for next scanline:
                curX = 1;
                dummyCycleToggle = !dummyCycleToggle;
            }
        } else if (scanline == 20 + vBlankAdd) {
            // Clear VBlank flag:
            setStatusFlag(STATUS_VBLANK, false);

            // Clear Sprite #0 hit flag:
            setStatusFlag(STATUS_SPRITE0HIT, false);
            hitSpr0 = false;
            spr0HitX = -1;
            spr0HitY = -1;

            if (fBgVisibility == 1 || fSpVisibility == 1) {
                // Update counters:
                cntFv = regFv;
                cntV = regV;
                cntH = regH;
                cntVt = regVt;
                cntHt = regHt;

                if (fBgVisibility == 1) {
                    // Render dummy scanline:
                    renderBgScanline(buffer, 0);
                }
            }

            if (fBgVisibility == 1 && fSpVisibility == 1) {
                // Check sprite 0 hit for first scanline:
                checkSprite0(0);
            }

            if (fBgVisibility == 1 || fSpVisibility == 1) {
                // Clock mapper IRQ Counter:
                nes.getMemoryMapper().clockIrqCounter();
            }
        } else if (scanline >= 21 + vBlankAdd && scanline <= 260) {
            // Render normally:
            if (fBgVisibility == 1) {
                if (!scanlineAlreadyRendered) {
                    // update scroll:
                    cntHt = regHt;
                    cntH = regH;
                    renderBgScanline(bgBuffer, scanline + 1 - 21);
                }
                scanlineAlreadyRendered = false;

                // Check for sprite 0 (next scanline):
                if (!hitSpr0 && fSpVisibility == 1) {
                    if (sprX[0] >= -7 && sprX[0] < 256 && sprY[0] + 1 <= (scanline - vBlankAdd + 1 - 21)
                            && (sprY[0] + 1 + (fSpriteSize == 0 ? 8 : 16)) >= (scanline - vBlankAdd + 1 - 21)) {
                        if (checkSprite0(scanline + vBlankAdd + 1 - 21)) {
                            ////System.out.println("found spr0. curscan="+scanline+" hitscan="+spr0HitY);
                            hitSpr0 = true;
                        }
                    }
                }
            }

            if (fBgVisibility == 1 || fSpVisibility == 1) {
                // Clock mapper IRQ Counter:
                nes.getMemoryMapper().clockIrqCounter();
            }
        } else if (scanline == 261 + vBlankAdd) {
            // Dead scanline, no rendering.
            // Set VINT:
            setStatusFlag(STATUS_VBLANK, true);
            requestEndFrame = true;
            nmiCounter = 9;

            // Wrap around:
            scanline = -1;    // will be incremented to 0
        }

        scanline++;
        regsToAddress();
        cntsToAddress();
    }

    public void startFrame() {
        int[] buffer = nes.getManager().getScreenView().getBuffer();

        // Set background color:
        int bgColor;
        if (fDispType == 0) {
            // Color display.
            // f_color determines color emphasis.
            // Use first entry of image palette as BG color.
            bgColor = imgPalette[0];
        } else {
            // Monochrome display.
            // f_color determines the bg color.
            switch (fColor) {
                case 0: // Black
                    bgColor = 0x00000;
                    break;
                case 1: // Green
                    bgColor = 0x00FF00;
                case 2: // Blue
                    bgColor = 0xFF0000;
                case 4: // Red
                    bgColor = 0x0000FF;
                case 3: // Invalid. Use black.
                default: // Invalid. Use black.
                    bgColor = 0x0;
            }
        }

        Arrays.fill(buffer, bgColor);
        Arrays.fill(pixrendered, 65);
    }

    public void endFrame() {
        int[] buffer = nes.getManager().getScreenView().getBuffer();

        // Draw spr#0 hit coordinates:
        if (showSpr0Hit) {
            // Spr 0 position:
            if (sprX[0] >= 0 && sprX[0] < 256 && sprY[0] >= 0 && sprY[0] < 240) {
                for (int i = 0; i < 256; i++) {
                    buffer[(sprY[0] << 8) + i] = 0xFF5555;
                }
                for (int i = 0; i < 240; i++) {
                    buffer[(i << 8) + sprX[0]] = 0xFF5555;
                }
            }
            // Hit position:
            if (spr0HitX >= 0 && spr0HitX < 256 && spr0HitY >= 0 && spr0HitY < 240) {
                for (int i = 0; i < 256; i++) {
                    buffer[(spr0HitY << 8) + i] = 0x55FF55;
                }
                for (int i = 0; i < 240; i++) {
                    buffer[(i << 8) + spr0HitX] = 0x55FF55;
                }
            }
        }

        // This is a bit lazy..
        // if either the sprites or the background should be clipped,
        // both are clipped after rendering is finished.
        if (clipTvColumn || fBgClipping == 0 || fSpClipping == 0) {
            // Clip left 8-pixels column:
            for (int y = 0; y < 240; y++) {
                for (int x = 0; x < 8; x++) {
                    buffer[(y << 8) + x] = 0;
                }
            }
        }

        if (clipTvColumn) {
            // Clip right 8-pixels column too:
            for (int y = 0; y < 240; y++) {
                for (int x = 0; x < 8; x++) {
                    buffer[(y << 8) + 255 - x] = 0;
                }
            }
        }

        // Clip top and bottom 8 pixels:
        if (clipTvRow) {
            for (int y = 0; y < 8; y++) {
                for (int x = 0; x < 256; x++) {
                    buffer[(y << 8) + x] = 0;
                    buffer[((239 - y) << 8) + x] = 0;
                }
            }
        }

        // Show sound buffer:
        if (showSoundBuffer && nes.getPapu().getLine() != null) {
            bufferSize = nes.getPapu().getLine().getBufferSize();
            available = nes.getPapu().getLine().available();
            scale = bufferSize / 256;

            for (int y = 0; y < 4; y++) {
                scanlineChanged[y] = true;
                for (int x = 0; x < 256; x++) {
                    if (x >= (available / scale)) {
                        buffer[y * 256 + x] = 0xFFFFFF;
                    } else {
                        buffer[y * 256 + x] = 0;
                    }
                }
            }
        }
    }

    public void updateControlReg1(int value) {
        triggerRendering();

        fNmiOnVBlank = (value >> 7) & 1;
        fSpriteSize = (value >> 5) & 1;
        fBgPatternTable = (value >> 4) & 1;
        fSpPatternTable = (value >> 3) & 1;
        fAddrInc = (value >> 2) & 1;
        fNameTableAddress = value & 3;

        regV = (value >> 1) & 1;
        regH = value & 1;
        regS = (value >> 4) & 1;
    }

    public void updateControlReg2(int value) {
        triggerRendering();

        fColor = (value >> 5) & 7;
        fSpVisibility = (value >> 4) & 1;
        fBgVisibility = (value >> 3) & 1;
        fSpClipping = (value >> 2) & 1;
        fBgClipping = (value >> 1) & 1;
        fDispType = value & 1;

        if (fDispType == 0) {
            nes.getPalTable().setEmphasis(fColor);
        }
        updatePalettes();
    }

    public void setStatusFlag(int flag, boolean value) {
        int n = 1 << flag;
        int memValue = nes.getCpuMemory().load(0x2002);
        memValue = ((memValue & (255 - n)) | (value ? n : 0));
        nes.getCpuMemory().write(0x2002, (short) memValue);
    }


    // CPU Register $2002:
    // Read the Status Register.

    public short readStatusRegister() {
        short tmp = nes.getCpuMemory().load(0x2002);

        // Reset scroll & VRAM Address toggle:
        firstWrite = true;

        // Clear VBlank flag:
        setStatusFlag(STATUS_VBLANK, false);

        // Fetch status data:
        return tmp;
    }

    // CPU Register $2003:
    // Write the SPR-RAM address that is used for sramWrite (Register 0x2004 in CPU memory map)
    public void writeSRAMAddress(short address) {
        sramAddress = address;
    }

    // CPU Register $2004 (R):
    // Read from SPR-RAM (Sprite RAM).
    // The address should be set first.
    public short sramLoad() {
        short tmp = sprMem.load(sramAddress);
        /*sramAddress++; // Increment address
        sramAddress%=0x100;*/
        return tmp;
    }

    // CPU Register $2004 (W):
    // Write to SPR-RAM (Sprite RAM).
    // The address should be set first.
    public void sramWrite(short value) {
        sprMem.write(sramAddress, value);
        spriteRamWriteUpdate(sramAddress, value);
        sramAddress++; // Increment address
        sramAddress %= 0x100;
    }

    // CPU Register $2005:
    // Write to scroll registers.
    // The first write is the vertical offset, the second is the
    // horizontal offset:
    public void scrollWrite(short value) {
        triggerRendering();
        if (firstWrite) {
            // First write, horizontal scroll:
            regHt = (value >> 3) & 31;
            regFh = value & 7;
        } else {
            // Second write, vertical scroll:
            regFv = value & 7;
            regVt = (value >> 3) & 31;
        }
        firstWrite = !firstWrite;
    }

    // CPU Register $2006:
    // Sets the adress used when reading/writing from/to VRAM.
    // The first write sets the high byte, the second the low byte.
    public void writeVRAMAddress(int address) {
        if (firstWrite) {
            regFv = (address >> 4) & 3;
            regV = (address >> 3) & 1;
            regH = (address >> 2) & 1;
            regVt = (regVt & 7) | ((address & 3) << 3);
        } else {
            triggerRendering();

            regVt = (regVt & 24) | ((address >> 5) & 7);
            regHt = address & 31;

            cntFv = regFv;
            cntV = regV;
            cntH = regH;
            cntVt = regVt;
            cntHt = regHt;

            checkSprite0(scanline - vBlankAdd + 1 - 21);
        }

        firstWrite = !firstWrite;

        // Invoke mapper latch:
        cntsToAddress();
        if (vramAddress < 0x2000) {
            nes.getMemoryMapper().latchAccess(vramAddress);
        }
    }

    // CPU Register $2007(R):
    // Read from PPU memory. The address should be set first.
    public short vramLoad() {
        cntsToAddress();
        regsToAddress();

        // If address is in range 0x0000-0x3EFF, return buffered values:
        if (vramAddress <= 0x3EFF) {
            short tmp = vramBufferedReadValue;

            // Update buffered value:
            if (vramAddress < 0x2000) {
                vramBufferedReadValue = ppuMem.load(vramAddress);
            } else {
                vramBufferedReadValue = mirroredLoad(vramAddress);
            }

            // Mapper latch access:
            if (vramAddress < 0x2000) {
                nes.getMemoryMapper().latchAccess(vramAddress);
            }

            // Increment by either 1 or 32, depending on d2 of Control Register 1:
            vramAddress += (fAddrInc == 1 ? 32 : 1);

            cntsFromAddress();
            regsFromAddress();
            return tmp; // Return the previous buffered value.
        }

        // No buffering in this mem range. Read normally.
        short tmp = mirroredLoad(vramAddress);

        // Increment by either 1 or 32, depending on d2 of Control Register 1:
        vramAddress += (fAddrInc == 1 ? 32 : 1);

        cntsFromAddress();
        regsFromAddress();

        return tmp;
    }

    // CPU Register $2007(W):
    // Write to PPU memory. The address should be set first.
    public void vramWrite(short value) {
        triggerRendering();
        cntsToAddress();
        regsToAddress();

        if (vramAddress >= 0x2000) {
            // Mirroring is used.
            mirroredWrite(vramAddress, value);
        } else {
            // Write normally.
            writeMem(vramAddress, value);

            // Invoke mapper latch:
            nes.getMemoryMapper().latchAccess(vramAddress);
        }

        // Increment by either 1 or 32, depending on d2 of Control Register 1:
        vramAddress += (fAddrInc == 1 ? 32 : 1);
        regsFromAddress();
        cntsFromAddress();
    }

    // CPU Register $4014:
    // Write 256 bytes of main memory
    // into Sprite RAM.
    public void sramDMA(short value) {
        Memory cpuMem = nes.getCpuMemory();
        int baseAddress = value * 0x100;
        short data;
        for (int i = sramAddress; i < 256; i++) {
            data = cpuMem.load(baseAddress + i);
            sprMem.write(i, data);
            spriteRamWriteUpdate(i, data);
        }

        nes.getCpu().haltCycles(513);
    }

    // Updates the scroll registers from a new VRAM address.

    private void regsFromAddress() {
        address = (vramTmpAddress >> 8) & 0xFF;
        regFv = (address >> 4) & 7;
        regV = (address >> 3) & 1;
        regH = (address >> 2) & 1;
        regVt = (regVt & 7) | ((address & 3) << 3);

        address = vramTmpAddress & 0xFF;
        regVt = (regVt & 24) | ((address >> 5) & 7);
        regHt = address & 31;
    }

    // Updates the scroll registers from a new VRAM address.
    private void cntsFromAddress() {
        address = (vramAddress >> 8) & 0xFF;
        cntFv = (address >> 4) & 3;
        cntV = (address >> 3) & 1;
        cntH = (address >> 2) & 1;
        cntVt = (cntVt & 7) | ((address & 3) << 3);

        address = vramAddress & 0xFF;
        cntVt = (cntVt & 24) | ((address >> 5) & 7);
        cntHt = address & 31;
    }

    private void regsToAddress() {
        b1 = (regFv & 7) << 4;
        b1 |= (regV & 1) << 3;
        b1 |= (regH & 1) << 2;
        b1 |= (regVt >> 3) & 3;

        b2 = (regVt & 7) << 5;
        b2 |= regHt & 31;

        vramTmpAddress = ((b1 << 8) | b2) & 0x7FFF;
    }

    private void cntsToAddress() {
        b1 = (cntFv & 7) << 4;
        b1 |= (cntV & 1) << 3;
        b1 |= (cntH & 1) << 2;
        b1 |= (cntVt >> 3) & 3;

        b2 = (cntVt & 7) << 5;
        b2 |= cntHt & 31;

        vramAddress = ((b1 << 8) | b2) & 0x7FFF;
    }

    private void incTileCounter(int count) {
        for (int i = count; i != 0; i--) {
            cntHt++;
            if (cntHt == 32) {
                cntHt = 0;
                cntVt++;
                if (cntVt >= 30) {
                    cntH++;
                    if (cntH == 2) {
                        cntH = 0;
                        cntV++;
                        if (cntV == 2) {
                            cntV = 0;
                            cntFv++;
                            cntFv &= 0x7;
                        }
                    }
                }
            }
        }
    }

    // Reads from memory, taking into account
    // mirroring/mapping of address ranges.
    private short mirroredLoad(int address) {
        return ppuMem.load(vramMirrorTable[address]);
    }

    // Writes to memory, taking into account
    // mirroring/mapping of address ranges.
    private void mirroredWrite(int address, short value) {
        if (address >= 0x3f00 && address < 0x3f20) {
            // Palette write mirroring.
            if (address == 0x3F00 || address == 0x3F10) {
                writeMem(0x3F00, value);
                writeMem(0x3F10, value);
            } else if (address == 0x3F04 || address == 0x3F14) {
                writeMem(0x3F04, value);
                writeMem(0x3F14, value);
            } else if (address == 0x3F08 || address == 0x3F18) {
                writeMem(0x3F08, value);
                writeMem(0x3F18, value);
            } else if (address == 0x3F0C || address == 0x3F1C) {
                writeMem(0x3F0C, value);
                writeMem(0x3F1C, value);
            } else {
                writeMem(address, value);
            }
        } else {
            // Use lookup table for mirrored address:
            if (address < vramMirrorTable.length) {
                writeMem(vramMirrorTable[address], value);
            } else {
                if (Globals.debug) {
                    // System.out.println("Invalid VRAM address: "+Misc.hex16(address));
                    nes.getCpu().setCrashed(true);
                }
            }
        }
    }

    public void triggerRendering() {
        if (scanline - vBlankAdd >= 21 && scanline - vBlankAdd <= 260) {
            // Render sprites, and combine:
            renderFramePartially(buffer, lastRenderedScanline + 1, scanline - vBlankAdd - 21 - lastRenderedScanline);

            // Set last rendered scanline:
            lastRenderedScanline = scanline - vBlankAdd - 21;
        }
    }

    private void renderFramePartially(int[] buffer, int startScan, int scanCount) {
        if (fSpVisibility == 1 && !Globals.disableSprites) {
            renderSpritesPartially(startScan, scanCount, true);
        }

        if (fBgVisibility == 1) {
            si = startScan << 8;
            ei = (startScan + scanCount) << 8;
            if (ei > 0xF000) {
                ei = 0xF000;
            }
            for (destIndex = si; destIndex < ei; destIndex++) {
                if (pixrendered[destIndex] > 0xFF) {
                    buffer[destIndex] = bgBuffer[destIndex];
                }
            }
        }

        if (fSpVisibility == 1 && !Globals.disableSprites) {
            renderSpritesPartially(startScan, scanCount, false);
        }

        BufferView screen = nes.getManager().getScreenView();
        if (screen.scalingEnabled() && !screen.useHWScaling() && !requestRenderAll) {
            // Check which scanlines have changed, to try to
            // speed up scaling:
            int j, jmax;
            if (startScan + scanCount > 240) {
                scanCount = 240 - startScan;
            }
            for (int i = startScan; i < startScan + scanCount; i++) {
                scanlineChanged[i] = false;
                si = i << 8;
                jmax = si + 256;
                for (j = si; j < jmax; j++) {
                    if (buffer[j] != oldFrame[j]) {
                        scanlineChanged[i] = true;
                        break;
                    }
                    oldFrame[j] = buffer[j];
                }
                System.arraycopy(buffer, j, oldFrame, j, jmax - j);
            }
        }

        validTileData = false;
    }

    private void renderBgScanline(int[] buffer, int scan) {
        baseTile = (regS == 0 ? 0 : 256);
        destIndex = (scan << 8) - regFh;
        curNt = nameTable1[cntV + cntV + cntH];

        cntHt = regHt;
        cntH = regH;
        curNt = nameTable1[cntV + cntV + cntH];

        if (scan < 240 && (scan - cntFv) >= 0) {
            tScanOffset = cntFv << 3;
            y = scan - cntFv;
            for (tile = 0; tile < 32; tile++) {
                if (scan >= 0) {
                    // Fetch tile & attrib data:
                    if (validTileData) {
                        // Get data from array:
                        t = scantile[tile];
                        tpix = t.pix;
                        att = attributes[tile];
                    } else {
                        // Fetch data:
                        t = ptTile[baseTile + nameTable[curNt].getTileIndex(cntHt, cntVt)];
                        tpix = t.pix;
                        att = nameTable[curNt].getAttrib(cntHt, cntVt);
                        scantile[tile] = t;
                        attributes[tile] = att;
                    }

                    // Render tile scanline:
                    sx = 0;
                    x = (tile << 3) - regFh;
                    if (x > -8) {
                        if (x < 0) {
                            destIndex -= x;
                            sx = -x;
                        }
                        if (t.opaque[cntFv]) {
                            for (; sx < 8; sx++) {
                                buffer[destIndex] = imgPalette[tpix[tScanOffset + sx] + att];
                                pixrendered[destIndex] |= 256;
                                destIndex++;
                            }
                        } else {
                            for (; sx < 8; sx++) {
                                col = tpix[tScanOffset + sx];
                                if (col != 0) {
                                    buffer[destIndex] = imgPalette[col + att];
                                    pixrendered[destIndex] |= 256;
                                }
                                destIndex++;
                            }
                        }
                    }
                }

                // Increase Horizontal Tile Counter:
                cntHt++;
                if (cntHt == 32) {
                    cntHt = 0;
                    cntH++;
                    cntH %= 2;
                    curNt = nameTable1[(cntV << 1) + cntH];
                }
            }

            // Tile data for one row should now have been fetched,
            // so the data in the array is valid.
            validTileData = true;
        }

        // update vertical scroll:
        cntFv++;
        if (cntFv == 8) {
            cntFv = 0;
            cntVt++;
            if (cntVt == 30) {
                cntVt = 0;
                cntV++;
                cntV %= 2;
                curNt = nameTable1[(cntV << 1) + cntH];
            } else if (cntVt == 32) {
                cntVt = 0;
            }

            // Invalidate fetched data:
            validTileData = false;
        }
    }

    private void renderSpritesPartially(int startscan, int scancount, boolean bgPri) {
        buffer = nes.getManager().getScreenView().getBuffer();
        if (fSpVisibility == 1) {
            int sprT1, sprT2;

            for (int i = 0; i < 64; i++) {
                if (bgPriority[i] == bgPri && sprX[i] >= 0 && sprX[i] < 256
                        && sprY[i] + 8 >= startscan && sprY[i] < startscan + scancount) {
                    // Show sprite.
                    if (fSpriteSize == 0) {
                        // 8x8 sprites
                        srcY1 = 0;
                        srcY2 = 8;

                        if (sprY[i] < startscan) {
                            srcY1 = startscan - sprY[i] - 1;
                        }

                        if (sprY[i] + 8 > startscan + scancount) {
                            srcY2 = startscan + scancount - sprY[i] + 1;
                        }

                        if (fSpPatternTable == 0) {
                            ptTile[sprTile[i]].render(0, srcY1, 8, srcY2, sprX[i], sprY[i] + 1,
                                    buffer, sprCol[i], sprPalette, horiFlip[i], vertFlip[i], i, pixrendered);
                        } else {
                            ptTile[sprTile[i] + 256].render(0, srcY1, 8, srcY2, sprX[i], sprY[i] + 1,
                                    buffer, sprCol[i], sprPalette, horiFlip[i], vertFlip[i], i, pixrendered);
                        }
                    } else {
                        // 8x16 sprites
                        int top = sprTile[i];
                        if ((top & 1) != 0) {
                            top = sprTile[i] - 1 + 256;
                        }

                        srcY1 = 0;
                        srcY2 = 8;

                        if (sprY[i] < startscan) {
                            srcY1 = startscan - sprY[i] - 1;
                        }

                        if (sprY[i] + 8 > startscan + scancount) {
                            srcY2 = startscan + scancount - sprY[i];
                        }

                        ptTile[top + (vertFlip[i] ? 1 : 0)].render(0, srcY1, 8, srcY2, sprX[i], sprY[i] + 1,
                                buffer, sprCol[i], sprPalette, horiFlip[i], vertFlip[i], i, pixrendered);

                        srcY1 = 0;
                        srcY2 = 8;

                        if (sprY[i] + 8 < startscan) {
                            srcY1 = startscan - (sprY[i] + 8 + 1);
                        }
                        if (sprY[i] + 16 > startscan + scancount) {
                            srcY2 = startscan + scancount - (sprY[i] + 8);
                        }
                        ptTile[top + (vertFlip[i] ? 0 : 1)].render(0, srcY1, 8, srcY2, sprX[i], sprY[i] + 1 + 8,
                                buffer, sprCol[i], sprPalette, horiFlip[i], vertFlip[i], i, pixrendered);
                    }
                }
            }
        }
    }

    private boolean checkSprite0(int scan) {
        spr0HitX = -1;
        spr0HitY = -1;

        int toffset;
        int tIndexAdd = (fSpPatternTable == 0 ? 0 : 256);
        int x, y;
        int bufferIndex;
        int col;
        boolean bgPri;
        Tile t;

        x = sprX[0];
        y = sprY[0] + 1;

        if (fSpriteSize == 0) { // 8x8 sprites.
            // Check range:
            if (y <= scan && y + 8 > scan && x >= -7 && x < 256) {
                // Sprite is in range.
                // Draw scanline:
                t = ptTile[sprTile[0] + tIndexAdd];
                col = sprCol[0];
                bgPri = bgPriority[0];

                if (vertFlip[0]) {
                    toffset = 7 - (scan - y);
                } else {
                    toffset = scan - y;
                }
                toffset *= 8;

                bufferIndex = scan * 256 + x;
                if (horiFlip[0]) {
                    for (int i = 7; i >= 0; i--) {
                        if (x >= 0 && x < 256) {
                            if (bufferIndex >= 0 && bufferIndex < 61440 && pixrendered[bufferIndex] != 0) {
                                if (t.pix[toffset + i] != 0) {
                                    spr0HitX = bufferIndex % 256;
                                    spr0HitY = scan;
                                    return true;
                                }
                            }
                        }
                        x++;
                        bufferIndex++;
                    }
                } else {
                    for (int i = 0; i < 8; i++) {
                        if (x >= 0 && x < 256) {
                            if (bufferIndex >= 0 && bufferIndex < 61440 && pixrendered[bufferIndex] != 0) {
                                if (t.pix[toffset + i] != 0) {
                                    spr0HitX = bufferIndex % 256;
                                    spr0HitY = scan;
                                    return true;
                                }
                            }
                        }
                        x++;
                        bufferIndex++;
                    }
                }
            }
        } else { // 8x16 sprites:
            // Check range:
            if (y <= scan && y + 16 > scan && x >= -7 && x < 256) {
                // Sprite is in range.
                // Draw scanline:
                if (vertFlip[0]) {
                    toffset = 15 - (scan - y);
                } else {
                    toffset = scan - y;
                }

                if (toffset < 8) {
                    // first half of sprite.
                    t = ptTile[sprTile[0] + (vertFlip[0] ? 1 : 0) + ((sprTile[0] & 1) != 0 ? 255 : 0)];
                } else {
                    // second half of sprite.
                    t = ptTile[sprTile[0] + (vertFlip[0] ? 0 : 1) + ((sprTile[0] & 1) != 0 ? 255 : 0)];
                    if (vertFlip[0]) {
                        toffset = 15 - toffset;
                    } else {
                        toffset -= 8;
                    }
                }
                toffset *= 8;
                col = sprCol[0];
                bgPri = bgPriority[0];

                bufferIndex = scan * 256 + x;
                if (horiFlip[0]) {
                    for (int i = 7; i >= 0; i--) {
                        if (x >= 0 && x < 256) {
                            if (bufferIndex >= 0 && bufferIndex < 61440 && pixrendered[bufferIndex] != 0) {
                                if (t.pix[toffset + i] != 0) {
                                    spr0HitX = bufferIndex % 256;
                                    spr0HitY = scan;
                                    return true;
                                }
                            }
                        }
                        x++;
                        bufferIndex++;
                    }
                } else {
                    for (int i = 0; i < 8; i++) {
                        if (x >= 0 && x < 256) {
                            if (bufferIndex >= 0 && bufferIndex < 61440 && pixrendered[bufferIndex] != 0) {
                                if (t.pix[toffset + i] != 0) {
                                    spr0HitX = bufferIndex % 256;
                                    spr0HitY = scan;
                                    return true;
                                }
                            }
                        }
                        x++;
                        bufferIndex++;
                    }
                }
            }
        }
        return false;
    }

    // Renders the contents of the
    // pattern table into an image.
    public void renderPattern() {
        BufferView scr = nes.getManager().getPatternView();
        int[] buffer = scr.getBuffer();

        int tIndex = 0;
        for (int j = 0; j < 2; j++) {
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    ptTile[tIndex].renderSimple(j * 128 + x * 8, y * 8, buffer, 0, sprPalette);
                    tIndex++;
                }
            }
        }
        nes.getManager().getPatternView().imageReady(false);
    }

    public void renderNameTables() {
        int[] buffer = nes.getManager().getNameTableView().getBuffer();
        if (fBgPatternTable == 0) {
            baseTile = 0;
        } else {
            baseTile = 256;
        }

        int ntx_max = 2;
        int nty_max = 2;

        if (currentMirroring == Rom.HORIZONTAL_MIRRORING) {
            ntx_max = 1;
        } else if (currentMirroring == Rom.VERTICAL_MIRRORING) {
            nty_max = 1;
        }

        for (int nty = 0; nty < nty_max; nty++) {
            for (int ntx = 0; ntx < ntx_max; ntx++) {
                int nt = nameTable1[nty * 2 + ntx];
                int x = ntx * 128;
                int y = nty * 120;

                // Render nametable:
                for (int ty = 0; ty < 30; ty++) {
                    for (int tx = 0; tx < 32; tx++) {
                        // ptTile[baseTile+nameTable[nt].getTileIndex(tx,ty)].render(0,0,4,4,x+tx*4,y+ty*4,
                        //     buffer,nameTable[nt].getAttrib(tx,ty),imgPalette,false,false,0,dummyPixPriTable);
                        ptTile[baseTile + nameTable[nt].getTileIndex(tx, ty)].renderSmall(
                                x + tx * 4, y + ty * 4, buffer, nameTable[nt].getAttrib(tx, ty), imgPalette);
                    }
                }
            }
        }

        if (currentMirroring == Rom.HORIZONTAL_MIRRORING) {
            // double horizontally:
            for (int y = 0; y < 240; y++) {
                for (int x = 0; x < 128; x++) {
                    buffer[(y << 8) + 128 + x] = buffer[(y << 8) + x];
                }
            }
        } else if (currentMirroring == Rom.VERTICAL_MIRRORING) {
            // double vertically:
            for (int y = 0; y < 120; y++) {
                for (int x = 0; x < 256; x++) {
                    buffer[(y << 8) + 0x7800 + x] = buffer[(y << 8) + x];
                }
            }
        }

        nes.getManager().getNameTableView().imageReady(false);
    }

    private void renderPalettes() {
        int[] buffer = nes.getManager().getImgPalView().getBuffer();
        for (int i = 0; i < 16; i++) {
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    buffer[y * 256 + i * 16 + x] = imgPalette[i];
                }
            }
        }

        buffer = nes.getManager().getSprPalView().getBuffer();
        for (int i = 0; i < 16; i++) {
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    buffer[y * 256 + i * 16 + x] = sprPalette[i];
                }
            }
        }

        nes.getManager().getImgPalView().imageReady(false);
        nes.getManager().getSprPalView().imageReady(false);
    }


    // This will write to PPU memory, and
    // update internally buffered data
    // appropriately.
    private void writeMem(int address, short value) {
        ppuMem.write(address, value);
        // Update internally buffered data:
        if (address < 0x2000) {
            ppuMem.write(address, value);
            patternWrite(address, value);
        } else if (address < 0x23c0) {
            nameTableWrite(nameTable1[0], address - 0x2000, value);
        } else if (address < 0x2400) {
            attribTableWrite(nameTable1[0], address - 0x23c0, value);
        } else if (address < 0x27c0) {
            nameTableWrite(nameTable1[1], address - 0x2400, value);
        } else if (address < 0x2800) {
            attribTableWrite(nameTable1[1], address - 0x27c0, value);
        } else if (address < 0x2bc0) {
            nameTableWrite(nameTable1[2], address - 0x2800, value);
        } else if (address < 0x2c00) {
            attribTableWrite(nameTable1[2], address - 0x2bc0, value);
        } else if (address < 0x2fc0) {
            nameTableWrite(nameTable1[3], address - 0x2c00, value);
        } else if (address < 0x3000) {
            attribTableWrite(nameTable1[3], address - 0x2fc0, value);
        } else if (address >= 0x3f00 && address < 0x3f20) {
            updatePalettes();
        }
    }

    // Reads data from $3f00 to $f20
    // into the two buffered palettes.
    public void updatePalettes() {
        final PaletteTable palTable = nes.getPalTable();
        for (int i = 0; i < 16; i++) {
            if (fDispType == 0) {
                imgPalette[i] = palTable.getEntry(ppuMem.load(0x3f00 + i) & 63);
            } else {
                imgPalette[i] = palTable.getEntry(ppuMem.load(0x3f00 + i) & 32);
            }
        }
        for (int i = 0; i < 16; i++) {
            if (fDispType == 0) {
                sprPalette[i] = palTable.getEntry(ppuMem.load(0x3f10 + i) & 63);
            } else {
                sprPalette[i] = palTable.getEntry(ppuMem.load(0x3f10 + i) & 32);
            }
        }
        // renderPalettes();
    }

    // Updates the internal pattern
    // table buffers with this new byte.
    public void patternWrite(int address, short value) {
        int tileIndex = address / 16;
        int leftOver = address % 16;
        if (leftOver < 8) {
            ptTile[tileIndex].setScanline(leftOver, value, ppuMem.load(address + 8));
        } else {
            ptTile[tileIndex].setScanline(leftOver - 8, ppuMem.load(address - 8), value);
        }
    }

    public void patternWrite(int address, short[] value, int offset, int length) {
        int tileIndex;
        int leftOver;

        for (int i = 0; i < length; i++) {
            tileIndex = (address + i) >> 4;
            leftOver = (address + i) % 16;

            if (leftOver < 8) {
                ptTile[tileIndex].setScanline(leftOver, value[offset + i], ppuMem.load(address + 8 + i));
            } else {
                ptTile[tileIndex].setScanline(leftOver - 8, ppuMem.load(address - 8 + i), value[offset + i]);
            }
        }
    }

    public void invalidateFrameCache() {
        // Clear the no-update scanline buffer:
        for (int i = 0; i < 240; i++) {
            scanlineChanged[i] = true;
        }
        java.util.Arrays.fill(oldFrame, -1);
        requestRenderAll = true;
    }

    // Updates the internal name table buffers
    // with this new byte.
    public void nameTableWrite(int index, int address, short value) {
        nameTable[index].writeTileIndex(address, value);

        // Update Sprite #0 hit:
        // updateSpr0Hit();
        checkSprite0(scanline + 1 - vBlankAdd - 21);
    }

    // Updates the internal pattern
    // table buffers with this new attribute
    // table byte.
    public void attribTableWrite(int index, int address, short value) {
        nameTable[index].writeAttrib(address, value);
    }

    // Updates the internally buffered sprite
    // data with this new byte of info.
    public void spriteRamWriteUpdate(int address, short value) {
        int tIndex = address / 4;

        if (tIndex == 0) {
            // updateSpr0Hit();
            checkSprite0(scanline + 1 - vBlankAdd - 21);
        }

        if (address % 4 == 0) {
            // Y coordinate
            sprY[tIndex] = value;
        } else if (address % 4 == 1) {
            // Tile index
            sprTile[tIndex] = value;
        } else if (address % 4 == 2) {
            // Attributes
            vertFlip[tIndex] = ((value & 0x80) != 0);
            horiFlip[tIndex] = ((value & 0x40) != 0);
            bgPriority[tIndex] = ((value & 0x20) != 0);
            sprCol[tIndex] = (value & 3) << 2;
        } else if (address % 4 == 3) {
            // X coordinate
            sprX[tIndex] = value;
        }
    }

    public void doNMI() {
        // Set VBlank flag:
        setStatusFlag(STATUS_VBLANK, true);
        // nes.getCpu().doNonMaskableInterrupt();
        nes.getCpu().requestIrq(Cpu.IRQ_NMI);
    }

    public int statusRegsToInt() {
        int ret = 0;
        ret = (fNmiOnVBlank) |
                (fSpriteSize << 1) |
                (fBgPatternTable << 2) |
                (fSpPatternTable << 3) |
                (fAddrInc << 4) |
                (fNameTableAddress << 5) |
                (fColor << 6) |
                (fSpVisibility << 7) |
                (fBgVisibility << 8) |
                (fSpClipping << 9) |
                (fBgClipping << 10) |
                (fDispType << 11);

        return ret;
    }

    public void statusRegsFromInt(int n) {
        fNmiOnVBlank = (n) & 0x1;
        fSpriteSize = (n >> 1) & 0x1;
        fBgPatternTable = (n >> 2) & 0x1;
        fSpPatternTable = (n >> 3) & 0x1;
        fAddrInc = (n >> 4) & 0x1;
        fNameTableAddress = (n >> 5) & 0x1;

        fColor = (n >> 6) & 0x1;
        fSpVisibility = (n >> 7) & 0x1;
        fBgVisibility = (n >> 8) & 0x1;
        fSpClipping = (n >> 9) & 0x1;
        fBgClipping = (n >> 10) & 0x1;
        fDispType = (n >> 11) & 0x1;
    }

    public void stateLoad(ByteBuffer buf) {
        // Check version:
        if (buf.readByte() == 1) {
            // Counters:
            cntFv = buf.readInt();
            cntV = buf.readInt();
            cntH = buf.readInt();
            cntVt = buf.readInt();
            cntHt = buf.readInt();

            // Registers:
            regFv = buf.readInt();
            regV = buf.readInt();
            regH = buf.readInt();
            regVt = buf.readInt();
            regHt = buf.readInt();
            regFh = buf.readInt();
            regS = buf.readInt();

            // VRAM address:
            vramAddress = buf.readInt();
            vramTmpAddress = buf.readInt();

            // Control/Status registers:
            statusRegsFromInt(buf.readInt());

            // VRAM I/O:
            vramBufferedReadValue = (short) buf.readInt();
            firstWrite = buf.readBoolean();
            // System.out.println("firstWrite: "+firstWrite);

            // Mirroring:
            // currentMirroring = -1;
            // setMirroring(buf.readInt());
            for (int i = 0; i < vramMirrorTable.length; i++) {
                vramMirrorTable[i] = buf.readInt();
            }

            // SPR-RAM I/O:
            sramAddress = (short) buf.readInt();

            // Rendering progression:
            curX = buf.readInt();
            scanline = buf.readInt();
            lastRenderedScanline = buf.readInt();

            // Misc:
            requestEndFrame = buf.readBoolean();
            nmiOk = buf.readBoolean();
            dummyCycleToggle = buf.readBoolean();
            nmiCounter = buf.readInt();
            short tmp = (short) buf.readInt();

            // Stuff used during rendering:
            for (int i = 0; i < bgBuffer.length; i++) {
                bgBuffer[i] = buf.readByte();
            }
            for (int i = 0; i < pixrendered.length; i++) {
                pixrendered[i] = buf.readByte();
            }

            // Name tables:
            for (int i = 0; i < 4; i++) {
                nameTable1[i] = buf.readByte();
                nameTable[i].stateLoad(buf);
            }

            // Pattern data:
            for (int i = 0; i < ptTile.length; i++) {
                ptTile[i].stateLoad(buf);
            }

            // Update internally stored stuff from VRAM memory:
			/*short[] mem = ppuMem.mem;

            // Palettes:
            for(int i=0x3f00;i<0x3f20;i++){
            writeMem(i,mem[i]);
            }
             */
            // Sprite data:
            short[] sprmem = nes.getSprMemory().getMem();
            for (int i = 0; i < sprmem.length; i++) {
                spriteRamWriteUpdate(i, sprmem[i]);
            }
        }
    }

    public void stateSave(ByteBuffer buf) {
        // Version:
        buf.putByte((short) 1);

        // Counters:
        buf.putInt(cntFv);
        buf.putInt(cntV);
        buf.putInt(cntH);
        buf.putInt(cntVt);
        buf.putInt(cntHt);

        // Registers:
        buf.putInt(regFv);
        buf.putInt(regV);
        buf.putInt(regH);
        buf.putInt(regVt);
        buf.putInt(regHt);
        buf.putInt(regFh);
        buf.putInt(regS);

        // VRAM address:
        buf.putInt(vramAddress);
        buf.putInt(vramTmpAddress);

        // Control/Status registers:
        buf.putInt(statusRegsToInt());

        // VRAM I/O:
        buf.putInt(vramBufferedReadValue);
        // System.out.println("firstWrite: "+firstWrite);
        buf.putBoolean(firstWrite);

        // Mirroring:
        // buf.putInt(currentMirroring);
        for (int i = 0; i < vramMirrorTable.length; i++) {
            buf.putInt(vramMirrorTable[i]);
        }

        // SPR-RAM I/O:
        buf.putInt(sramAddress);

        // Rendering progression:
        buf.putInt(curX);
        buf.putInt(scanline);
        buf.putInt(lastRenderedScanline);

        // Misc:
        buf.putBoolean(requestEndFrame);
        buf.putBoolean(nmiOk);
        buf.putBoolean(dummyCycleToggle);
        buf.putInt(nmiCounter);
        // buf.putInt(tmp);

        // Stuff used during rendering:
        for (int j : bgBuffer) {
            buf.putByte((short) j);
        }
        for (int j : pixrendered) {
            buf.putByte((short) j);
        }

        // Name tables:
        for (int i = 0; i < 4; i++) {
            buf.putByte((short) nameTable1[i]);
            nameTable[i].stateSave(buf);
        }

        // Pattern data:
        for (Tile value : ptTile) {
            value.stateSave(buf);
        }
    }

    // Reset PPU:
    public void reset() {
        ppuMem.reset();
        sprMem.reset();

        vramBufferedReadValue = 0;
        sramAddress = 0;
        curX = 0;
        scanline = 0;
        lastRenderedScanline = 0;
        spr0HitX = 0;
        spr0HitY = 0;
        mapperIrqCounter = 0;

        currentMirroring = -1;

        firstWrite = true;
        requestEndFrame = false;
        nmiOk = false;
        hitSpr0 = false;
        dummyCycleToggle = false;
        validTileData = false;
        nmiCounter = 0;
        att = 0;

        // Control Flags Register 1:
        fNmiOnVBlank = 0;    // NMI on VBlank. 0=disable, 1=enable
        fSpriteSize = 0;     // Sprite size. 0=8x8, 1=8x16
        fBgPatternTable = 0; // Background Pattern Table address. 0=0x0000,1=0x1000
        fSpPatternTable = 0; // Sprite Pattern Table address. 0=0x0000,1=0x1000
        fAddrInc = 0;        // PPU Address Increment. 0=1,1=32
        fNameTableAddress = 0;    // Name Table Address. 0=0x2000,1=0x2400,2=0x2800,3=0x2C00

        // Control Flags Register 2:
        fColor = 0;          // Background color. 0=black, 1=blue, 2=green, 4=red
        fSpVisibility = 0;   // Sprite visibility. 0=not displayed,1=displayed
        fBgVisibility = 0;   // Background visibility. 0=Not Displayed,1=displayed
        fSpClipping = 0;     // Sprite clipping. 0=Sprites invisible in left 8-pixel column,1=No clipping
        fBgClipping = 0;     // Background clipping. 0=BG invisible in left 8-pixel column, 1=No clipping
        fDispType = 0;       // Display type. 0=color, 1=monochrome

        // Counters:
        cntFv = 0;
        cntV = 0;
        cntH = 0;
        cntVt = 0;
        cntHt = 0;

        // Registers:
        regFv = 0;
        regV = 0;
        regH = 0;
        regVt = 0;
        regHt = 0;
        regFh = 0;
        regS = 0;

        java.util.Arrays.fill(scanlineChanged, true);
        java.util.Arrays.fill(oldFrame, -1);

        // Initialize stuff:
        init();
    }

    public void destroy() {
        nes = null;
        ppuMem = null;
        sprMem = null;
        scantile = null;
    }

    public boolean isRequestRenderAll() {
        return requestRenderAll;
    }

    public void setRequestRenderAll(boolean requestRenderAll) {
        this.requestRenderAll = requestRenderAll;
    }

    public boolean[] getScanlineChanged() {
        return scanlineChanged;
    }

    public void setScanlineChanged(boolean[] scanlineChanged) {
        this.scanlineChanged = scanlineChanged;
    }

    public int[] getBuffer() {
        return buffer;
    }

    public void setBuffer(int[] buffer) {
        this.buffer = buffer;
    }

    public boolean isShowSoundBuffer() {
        return showSoundBuffer;
    }

    public void setShowSoundBuffer(boolean showSoundBuffer) {
        this.showSoundBuffer = showSoundBuffer;
    }
}
