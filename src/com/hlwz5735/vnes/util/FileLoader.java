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

package com.hlwz5735.vnes.util;

import com.hlwz5735.vnes.common.Globals;
import com.hlwz5735.vnes.gui.UI;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FileLoader {

    // Load a file.
    public short[] loadFile(String fileName, UI ui) {
        int flen;
        byte[] tmp = new byte[2048];

        // Read file:
        try {
            InputStream in;
            in = getClass().getResourceAsStream(fileName);

            if (in == null) {
                // Try another approach.
                in = Files.newInputStream(Paths.get(fileName));
            }
            ZipInputStream zis = null;
            boolean zip = false;

            int pos = 0;
            int readbyte = 0;

            if (!(in instanceof FileInputStream)) {


                long total = -1;

                if (fileName.endsWith(".zip")) {
                    zis = new ZipInputStream(in);
                    ZipEntry entry = zis.getNextEntry();
                    total = entry.getSize();
                    zip = true;
                } else if (Globals.appletMode && ui != null) {
                    // Can't get the file size, so use the applet parameter
                    total = ui.getRomFileSize();
                }

                long progress = -1;
                while (readbyte != -1) {
                    readbyte = zip ? zis.read(tmp, pos, tmp.length - pos) : in.read(tmp, pos, tmp.length - pos);
                    if (readbyte != -1) {
                        if (pos >= tmp.length) {
                            byte[] newtmp = new byte[tmp.length + 32768];
                            System.arraycopy(tmp, 0, newtmp, 0, tmp.length);
                            tmp = newtmp;
                        }
                        pos += readbyte;
                    }

                    if (total > 0 && ((pos * 100L) / total) > progress) {
                        progress = (pos * 100L) / total;
                        if (ui != null) {
                            ui.showLoadProgress((int) progress);
                        }
                    }

                }

            } else {

                // This is easy, can find the file size since it's
                // in the local file system.
                File f = new File(fileName);
                int count = 0;
                int total = (int) (f.length());
                tmp = new byte[total];
                while (count < total) {
                    count += in.read(tmp, count, total - count);
                }
                pos = total;

            }

            // Put into array without any padding:
            byte[] newtmp = new byte[pos];
            System.arraycopy(tmp, 0, newtmp, 0, pos);
            tmp = newtmp;

            // File size:
            flen = tmp.length;

        } catch (IOException ioe) {

            // Something went wrong.
            ioe.printStackTrace();
            return null;

        }

        short[] ret = new short[flen];
        for (int i = 0; i < flen; i++) {
            ret[i] = (short) (tmp[i] & 255);
        }
        return ret;
    }
}
