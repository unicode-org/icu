/**
 *******************************************************************************
 * Copyright (C) 2001-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.richtext.textpanel;

final class ThaiKeyRemap extends KeyRemap {

    public char remap(char c) {

        switch (c) {
            case '`': return '\u005f';
            case '~': return '\u0025';
            case '1': return '\u0e45';
            case '!': return '\u002b';
            case '2': return '\u002f';
            case '@': return '\u0e51';
            case '3': return '\u002d';
            case '#': return '\u0e52';
            case '4': return '\u0e20';
            case '$': return '\u0e53';
            case '5': return '\u0e16';
            case '%': return '\u0e54';
            case '6': return '\u0e38';
            case '^': return '\u0e39';
            case '7': return '\u0e36';
            case '&': return '\u0e3f';
            case '8': return '\u0e04';
            case '*': return '\u0e55';
            case '9': return '\u0e15';
            case '(': return '\u0e56';
            case '0': return '\u0e08';
            case ')': return '\u0e57';
            case '-': return '\u0e02';
            case '_': return '\u0e58';
            case '=': return '\u0e08';
            case '+': return '\u0e59';
            case 'q': return '\u0e46';
            case 'Q': return '\u0e50';
            case 'w': return '\u0e44';
            case 'W': return '\u0022';
            case 'e': return '\u0e33';
            case 'E': return '\u0e0e';
            case 'r': return '\u0e1e';
            case 'R': return '\u0e11';
            case 't': return '\u0e30';
            case 'T': return '\u0e18';
            case 'y': return '\u0e31';
            case 'Y': return '\u0e4d';
            case 'u': return '\u0e35';
            case 'U': return '\u0e4a';
            case 'i': return '\u0e23';
            case 'I': return '\u0e13';
            case 'o': return '\u0e19';
            case 'O': return '\u0e2f';
            case 'p': return '\u0e22';
            case 'P': return '\u0e0d';
            case '[': return '\u0e1a';
            case '{': return '\u0e10';
            case ']': return '\u0e25';
            case '}': return '\u002c';
            case '\\': return '\u0e03';
            case '|': return '\u0e05';
            case 'a': return '\u0e1f';
            case 'A': return '\u0e24';
            case 's': return '\u0e2b';
            case 'S': return '\u0e06';
            case 'd': return '\u0e01';
            case 'D': return '\u0e0f';
            case 'f': return '\u0e14';
            case 'F': return '\u0e42';
            case 'g': return '\u0e40';
            case 'G': return '\u0e0c';
            case 'h': return '\u0e49';
            case 'H': return '\u0e47';
            case 'j': return '\u0e48';
            case 'J': return '\u0e4b';
            case 'k': return '\u0e32';
            case 'K': return '\u0e29';
            case 'l': return '\u0e2a';
            case 'L': return '\u0e28';
            case ';': return '\u0e27';
            case ':': return '\u0e0b';
            case '\'': return '\u0e07';
            case '\"': return '\u002e';
            case 'z': return '\u0e1c';
            case 'Z': return '\u0028';
            case 'x': return '\u0e1b';
            case 'X': return '\u0029';
            case 'c': return '\u0e41';
            case 'C': return '\u0e09';
            case 'v': return '\u0e2d';
            case 'V': return '\u0e2e';
            case 'b': return '\u0e34';
            case 'B': return '\u0e3a';
            case 'n': return '\u0e37';
            case 'N': return '\u0e4c';
            case 'm': return '\u0e17';
            case 'M': return '\u003f';
            case ',': return '\u0e21';
            case '<': return '\u0e12';
            case '.': return '\u0e43';
            case '>': return '\u0e2c';
            case '/': return '\u0e1d';
            case '?': return '\u0e26';
        }

        return c;
    }
}
