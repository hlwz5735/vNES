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

package com.hlwz5735.vnes.input;

import com.hlwz5735.vnes.core.Nes;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Arrays;

public class KbInputHandler implements KeyListener, InputHandler {
    /** 控制器的ID，主手柄是0 */
    private final int id;

    private final Nes nes;

    /** 保存所有键盘按键状态的数组，最多支持255个按键编码（够用了） */
    private final boolean[] allKeysState = new boolean[255];

    /** 手柄按钮ID到键盘按键编码的映射表 */
    private final int[] keyMapping = new int[NUM_KEYS];

    public KbInputHandler(Nes nes, int id) {
        this.nes = nes;
        this.id = id;
    }

    @Override
    public short getKeyState(int padKey) {
        return (short) (allKeysState[keyMapping[padKey]] ? 0x41 : 0x40);
    }

    @Override
    public void mapKey(int padKey, int kbKeycode) {
        keyMapping[padKey] = kbKeycode;
    }

    @Override
    public void reset() {
        Arrays.fill(this.allKeysState, false);
    }

    @Override
    public void update() {
        // doesn't do anything.
    }

    public void destroy() {
    }

    @Override
    public void keyPressed(KeyEvent ke) {
        int kc = ke.getKeyCode();
        if (kc >= allKeysState.length) {
            return;
        }

        allKeysState[kc] = true;

        // Can't hold both left & right or up & down at same time:
        if (kc == keyMapping[InputHandler.KEY_LEFT]) {
            allKeysState[keyMapping[InputHandler.KEY_RIGHT]] = false;
        } else if (kc == keyMapping[InputHandler.KEY_RIGHT]) {
            allKeysState[keyMapping[InputHandler.KEY_LEFT]] = false;
        } else if (kc == keyMapping[InputHandler.KEY_UP]) {
            allKeysState[keyMapping[InputHandler.KEY_DOWN]] = false;
        } else if (kc == keyMapping[InputHandler.KEY_DOWN]) {
            allKeysState[keyMapping[InputHandler.KEY_UP]] = false;
        }
    }

    @Override
    public void keyReleased(KeyEvent ke) {
        int kc = ke.getKeyCode();
        if (kc >= allKeysState.length) {
            return;
        }

        allKeysState[kc] = false;

        // 主手柄的复位键按下
        if (id == 0 && kc == KeyEvent.VK_F5) {
            if (nes.isRunning()) {
                nes.stopEmulation();
                nes.reset();
                nes.reloadRom();
                nes.startEmulation();
            }
        }
    }

    @Override
    public void keyTyped(KeyEvent ke) {
        // Ignore.
    }
}
