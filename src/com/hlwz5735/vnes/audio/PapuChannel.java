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

package com.hlwz5735.vnes.audio;

/**
 * 代表一个音频通道
 */
public interface PapuChannel {

    /**
     * 写入寄存器
     * @param address 地址
     * @param value 值
     */
    void writeReg(int address, int value);

    /** 设置是否启用此通道 */
    void setEnabled(boolean value);

    /** 获取是否启用此通道 */
    boolean isEnabled();

    /** 通道复位 */
    void reset();

    /** 获取通道长度状态 */
    int getLengthStatus();
}
