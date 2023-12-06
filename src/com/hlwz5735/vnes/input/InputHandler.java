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

public interface InputHandler {

    // Joypad keys:
    int KEY_A = 0;
    int KEY_B = 1;
    int KEY_START = 2;
    int KEY_SELECT = 3;
    int KEY_UP = 4;
    int KEY_DOWN = 5;
    int KEY_LEFT = 6;
    int KEY_RIGHT = 7;

    /** 一个控制器的按键总数量 */
    int NUM_KEYS = 8;

    /**
     * 获取某个按键的状态信息
     * @param padKey 按键的Key
     * @return 按键状态 0x41 代表按下，0x40 代表未按下
     */
    short getKeyState(int padKey);

    /** 将控制器按键Key和物理键盘的按键Key绑定起来 */
    void mapKey(int padKey, int deviceKey);

    /** 复位所有按键状态 */
    void reset();

    /** 更新按键状态 */
    void update();
}
