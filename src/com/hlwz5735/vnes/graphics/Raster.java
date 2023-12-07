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

/**
 * 光栅化器
 * 貌似目前没有用
 */
public class Raster {

    // 图块数据
    public int[] data;
    public int width;
    public int height;

    public Raster(int[] data, int w, int h) {
        this.data = data;
        width = w;
        height = h;
    }

    public Raster(int w, int h) {
        data = new int[w * h];
        width = w;
        height = h;
    }

    /**
     * 将图块绘制到指定位置
     * @param srcRaster 源光栅化器
     * @param srcx 源X坐标
     * @param srcy 源Y坐标
     * @param dstx 目标X坐标
     * @param dsty 目标Y坐标
     * @param w 宽度
     * @param h 高度
     */
    public void drawTile(Raster srcRaster, int srcx, int srcy, int dstx, int dsty, int w, int h) {
        int[] src = srcRaster.data;
        int srcIdx;
        int destIdx;
        int tmp;

        for (int y = 0; y < h; y++) {
            srcIdx = (srcy + y) * srcRaster.width + srcx;
            destIdx = (dsty + y) * width + dstx;
            for (int x = 0; x < w; x++) {
                if ((tmp = src[srcIdx]) != 0) {
                    data[destIdx] = tmp;
                }
                srcIdx++;
                destIdx++;
            }
        }
    }
}
