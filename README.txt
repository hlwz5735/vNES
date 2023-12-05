+-----------------------------------------------------------------------------+
| vNES 2.13                                                                   |
| Copyright 2006-2010 Jamie Sanders                                         |
|                                                                             |
| This program is free software: you can redistribute it and/or modify it     |
| under the terms of the GNU General Public License as published by the       |
| Free Software Foundation, either version 3 of the License, or               |
| (at your option) any later version.                                         |
|                                                                             |
| This program is distributed in the hope that it will be useful, but WITHOUT |
| ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or       |
| FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for   |
| more details.                                                               |
|                                                                             |
| You should have received a copy of the GNU General Public License along     |
| with this program.  If not, see <http://www.gnu.org/licenses/>.             |
+-----------------------------------------------------------------------------+

+-----------------------------------------------------------------------------+
| Using vNES on Your Website                                                  |
+-----------------------------------------------------------------------------+

As old as it is, I still reccommend using the <applet> tag with vNES for use
on the web. There's four different ways that browsers handle the <object> tag
and it's simply not stable enough for this use. I'll wait until the W3 sorts
out that mess.

<applet code="vNES.class" archive="vNES_213.jar" width="512" height="480">

<!-- Filename of ROM. No extention is checked. Should be iNES format. -->
<param name="rom" value="">
<!-- Filesize of the ROM, in bytes. -->
<param name="romsize" value="">
<!-- Sound on/off. -->
<param name="sound" value="">
<!-- Psuedo-Stereo Sound on/off -->
<param name="stereo" value="">
<!-- Scanlines on/off -->
<param name="scanlines" value="">
<!-- Scale on/off. Goes up to 2x for now, will be higher soon. -->
<param name="scale" value="">
<!-- FPS Counter on/off -->
<param name="fps" value="">

<!-- Keyboard Setup for Player 1. Uses Virtual Keyboard. -->
<param name="p1_up" value="">
<param name="p1_down" value="">
<param name="p1_left" value="">
<param name="p1_right" value="">
<param name="p1_a" value="">
<param name="p1_b" value="">
<param name="p1_start" value="">
<param name="p1_select" value="">

<!-- Keyboard Setup for Player 1. Uses Virtual Keyboard. -->
<param name="p2_up" value="">
<param name="p2_down" value="">
<param name="p2_left" value="">
<param name="p2_right" value="">
<param name="p2_a" value="">
<param name="p2_b" value="">
<param name="p2_start" value="">
<param name="p2_select" value="">
</applet>

Here's the values you can use with the keyboard config:

NUMPAD0
NUMPAD1
NUMPAD2
NUMPAD3
NUMPAD4
NUMPAD5
NUMPAD6
NUMPAD7
NUMPAD8
NUMPAD9
MULTIPLY
DIVIDE
ADD
SUBTRACT
DECIMAL

0 1 2 3 4 5 6 7 8 9
A B C D E F G H I J K L M N O P Q R S T U V W X Y Z

UP
DOWN
LEFT
RIGHT
BACK_SPACE
TAB
ENTER
SHIFT
CONTROL
ALT
PAUSE
ESCAPE
OPEN_BRACKET
CLOSE_BRACKET
BACK_SLASH
SEMICOLON
QUOTE
COMMA
PERIOD
SLASH
SPACE
PAGE_UP
PAGE_DOWN
HOME
END
INSERT
DELETE

+-----------------------------------------------------------------------------+
| For updates and more detailed documentation, see www.openemulation.com      |
+-----------------------------------------------------------------------------+
