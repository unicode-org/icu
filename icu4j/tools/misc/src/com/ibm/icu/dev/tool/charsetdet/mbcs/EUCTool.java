// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 ***********************************************************************
 *
 * Copyright (C) 2005-2012, International Business Machines Corporation and
 * others. All Rights Reserved.
 *
 ***********************************************************************
 *
 * euc_tool
 *
 *    This tool produces the character usage frequency statistics for the EUC family
 *    of charsets, for use by the ICU charset detectors.
 *
 *    usage:  java euc_tool [-d] [directory path]
 *
 *        -d:   Produce the data in a form to be exported to the ICU implementation
 *              Default is to produce an informative dump.
 *
 *        directory path
 *              Source directory for the files to be analyzed.
 *              Default is the current directory.
 *              There should be three subdirectories under the specified directory, one
 *              each for EUC_JP, EUC_CN and EUC_KR.  Within each of these subdirectories
 *              should be text files in the specified encoding.
 *
 */

package com.ibm.icu.dev.tool.charsetdet.mbcs;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class EUCTool {

    // The file buffer and file data length need to be out in class member variables
    //  so that the code lifted from charSet detection for scanning the multi-byte chars
    //  can see them conveniently.
    byte []    buf = new byte[1000000];
    int        fileSize;

    boolean    option_d = false;    // data option.  Produce exportable data
    boolean    option_v = true;     // verbose informaional output.



    public static void main(String[] args) {
        EUCTool  This = new EUCTool();
        This.Main(args);
    }



    void Main(String[] args) {
        int i;

        //
        //   Command Line Option Handling
        //
        String     dirName  = ".";
        for (i=0; i<args.length; i++) {
            if (args[i].equals("-d")) {
                option_d = true;
                option_v = false;
                continue;
            }
            if (args[i].startsWith("-")) {
                System.err.println("Unrecongized option: " + args[i]);
                System.exit(-1);
            }
            dirName = args[i];
        }

        //
        //  Verify that the specified directory exists.
        //
        File dir = new File(dirName);
        if (dir.isDirectory() == false) {
            System.err.println("\"" + dirName + "\" is not a directory");
            System.exit(-1);
        }

        //
        //  Do each subdirectory of the specified directory.  There should be
        //    one per each encoding - euc-kr, euc-cn, euc-jp
        //
        File[] dirs  = dir.listFiles();
        for (i=0; i<dirs.length; i++) {
            if (dirs[i].isDirectory()) {
                String nam = dirs[i].getName();
                if (nam.equalsIgnoreCase("CVS")) {
                    continue;
                }
                processDir(dirs[i]);
            }
        }
    }

    //
    // Collect statistics from all ordinary files in a specified directory.
    //
    void processDir(File dir) {
        int      totalMbcsChars  = 0;
        HashMap  m = new HashMap(10000);
        int      i;

        System.out.println(dir.getName());
        File[] files = dir.listFiles();
        for (i=0; i<files.length; i++) {
            FileInputStream is = null;
            try {
                if (files[i].isFile()) {
                    is = new FileInputStream(files[i]);
                    fileSize = is.read(buf);
                    if (option_v) {
                        System.out.println(files[i].getPath());
                        System.out.println("  " + fileSize + " bytes.");
                    }
                    iteratedChar ichar = new iteratedChar();
                    int fileChars     = 0;
                    int fileMbcsChars = 0;
                    int errs          = 0;

                    while (nextChar(ichar)) {
                        if (ichar.error == true) {
                            errs++;
                            continue;
                        }
                        fileChars++;
                        if (ichar.charValue > 255) {
                            fileMbcsChars++;
                            totalMbcsChars++;
                        }
                        if (ichar.charValue <= 255) {
                            // Don't keep occurence statistics for the single byte range
                            continue;
                        }

                        //
                        //  Frequency of occurence statistics are accumulated in a map.
                        //
                        ChEl  keyEl = new ChEl(ichar.charValue, 0);
                        ChEl  valEl = (ChEl)m.get(keyEl);
                        if (valEl == null) {
                            m.put(keyEl, keyEl);
                            valEl = keyEl;
                        }
                        valEl.occurences++;
                    }
                    if (option_v) {
                        System.out.println("  " + fileChars     + " Chars");
                        System.out.println("  " + fileMbcsChars + " mbcs Chars");
                        System.out.println("  " + errs          + " errors");
                        System.out.println("\n");
                    }
                }
            }
            catch (Exception e) {
                System.err.println("Exception:" + e);

            }
            finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (Exception e) {
                        // ignore
                    }
                }
            }
        }

        //
        //  We've processed through all of the files.
        //     sort and dump out the frequency statistics.
        //
        Object [] encounteredChars = m.values().toArray();
        Arrays.sort(encounteredChars);
        int cumulativeChars = 0;
        int cumulativePercent = 0;
        if (option_v) {
            System.out.println("# <char code> <occurences>  <Cumulative %>");
            for (i=0; i<encounteredChars.length; i++) {
                ChEl c = (ChEl)encounteredChars[i];
                cumulativeChars += c.occurences;
                cumulativePercent = cumulativeChars*100/totalMbcsChars;
                System.out.println(i + "   " + Integer.toHexString(c.charCode) + "        " 
                        + c.occurences + "         " + cumulativePercent);
            }
        }
        if (option_d) {
            //
            //   Output the list of characters formatted for pasting into a
            //     Java source code array initializer.
            //     Resort into order based on the character code value, not
            //      on frequency of occurence.
            //
            List  charList = new ArrayList();
            
            for (i=0; i<100 && cumulativePercent<50; i++) {
                ChEl c = (ChEl)encounteredChars[i];
                cumulativeChars += c.occurences;
                cumulativePercent = cumulativeChars*100/totalMbcsChars;
                charList.add(new Integer(c.charCode));
            }
            Object [] sortedChars = charList.toArray();
            Arrays.sort(sortedChars);
            
            System.out.print("          {");
            for (i=0; i<sortedChars.length; i++) {
                if (i != 0) {
                    System.out.print(", ");
                    if ((i)%10 == 0) {
                        System.out.print("\n           ");
                    }
                }
                int cp = ((Integer)sortedChars[i]).intValue();
                System.out.print("0x" + Integer.toHexString(cp));
            }
            System.out.println("};");
        }
    }
    
    //
    //  This is a little class containing a
    //    multi-byte character value and an occurence count for that char.
    //  Instances of this class are kept in the collection that accumulates statistics
    //
    //  WARNING:  this class's natural ordering (from Comparable) and equals()
    //            are inconsistent.

    static class ChEl implements Comparable {
        int charCode;
        int occurences;

        ChEl(int c, int o) {
            charCode = c;
            occurences = o;
        }

        // Equals needs to work with a map, with the charCode as the key.
        //   For insertion/lookup, we care about the char code only, not the occurence count.
        public boolean equals(Object other) {
            ChEl o = (ChEl)other;
            return o.charCode == this.charCode;
        }

        // Hashcode needs to be compatible with equals
        //   We're using this in a hashMap!
        public int hashCode() {
            return charCode;
        }

        // We want to be able to sort the results by frequency of occurence
        //   Compare backwards.  We want most frequent chars first.
        public int compareTo(Object other) {
            ChEl o = (ChEl)other;
            return (this.occurences> o.occurences? -1 :
                   (this.occurences==o.occurences?  0 : 1));
        }

    }

    //
    // iteratedChar is copied and slightly hacked from the similar calss in CharsetRecog_mbcs
    //              Pulls out one logical char according to the rules of EUC encoding.
    //
    class iteratedChar {
        int             charValue = 0;             // The char value is a value from the encoding.
                                                   //   It's meaning is not well defined, other than
                                                   //   different encodings
        int             index     = 0;
        int             nextIndex = 0;
        boolean         error     = false;
        boolean         done      = false;

        void reset() {
            charValue = 0;
            index     = -1;
            nextIndex = 0;
            error     = false;
            done      = false;
        }

        int nextByte() {
            if (nextIndex >= fileSize) {
                done = true;
                return -1;
            }
            int byteValue = (int)buf[nextIndex++] & 0x00ff;
            return byteValue;
        }
    }


    boolean nextChar(iteratedChar it) {
        it.index = it.nextIndex;
        it.error = false;
        int firstByte  = 0;
        int secondByte = 0;
        int thirdByte  = 0;
        int fourthByte = 0;

        buildChar: {
            firstByte = it.charValue = it.nextByte();
            if (firstByte < 0) {
                // Ran off the end of the input data
                it.done = true;
                break buildChar;
            }
            if (firstByte <= 0x8d) {
                // single byte char
                break buildChar;
            }

            secondByte = it.nextByte();
            it.charValue = (it.charValue << 8) | secondByte;

            if (firstByte >= 0xA1 && firstByte <= 0xfe) {
                // Two byte Char
                if (secondByte < 0xa1) {
                    it.error = true;
                }
                break buildChar;
            }
            if (firstByte == 0x8e) {
                // Code Set 2.
                //   In EUC-JP, total char size is 2 bytes, only one byte of actual char value.
                //   In EUC-TW, total char size is 4 bytes, three bytes contribute to char value.
                // We don't know which we've got.
                // Treat it like EUC-JP.  If the data really was EUC-TW, the following two
                //   bytes will look like a well formed 2 byte char.
                if (secondByte < 0xa1) {
                    it.error = true;
                }
                break buildChar;
            }

            if (firstByte == 0x8f) {
                // Code set 3.
                // Three byte total char size, two bytes of actual char value.
                thirdByte    = it.nextByte();
                it.charValue = (it.charValue << 8) | thirdByte;
                if (thirdByte < 0xa1) {
                    it.error = true;
                }
            }
            
        }
        if (it.error) {
            System.out.println("Error " + Integer.toHexString(firstByte) + " " + Integer.toHexString(secondByte)
                    + " " +  Integer.toHexString(thirdByte) + " " + Integer.toHexString(fourthByte));
        }
        return (it.done == false);
    }
}




