// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 1998-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.layout;

/**
 * @author emader
 *
 */
public class OpenTypeTagBuilder
{
    private static String[] tableTags = {
        "acnt",
        "avar",
        "BASE",
        "bdat",
        "bhed",
        "bloc",
        "bsln",
        "CFF ",
        "cmap",
        "cvar",
        "cvt ",
        "DSIG",
        "EBDT",
        "EBLC",
        "EBSC",
        "fdsc",
        "feat",
        "fmtx",
        "fpgm",
        "fvar",
        "gasp",
        "GDEF",
        "glyf",
        "GPOS",
        "GSUB",
        "gvar",
        "hdmx",
        "head",
        "hhea",
        "hmtx",
        "hsty",
        "just",
        "JSTF",
        "kern",
        "lcar",
        "loca",
        "LTSH",
        "maxp",
        "mort",
        "morx",
        "name",
        "opbd",
        "OS/2",
        "PCLT",
        "post",
        "prep",
        "prop",
        "trak",
        "VDMX",
        "vhea",
        "vmtx",
        "VORG",
        "Zapf"
    };

    private static String[] featureTags = {
        "aalt",
        "abvf",
        "abvm",
        "abvs",
        "afrc",
        "akhn",
        "blwf",
        "blwm",
        "blws",
        "calt",
        "case",
        "ccmp",
        "clig",
        "cpsp",
        "cswh",
        "curs",
        "c2sc",
        "c2pc",
        "dist",
        "dlig",
        "dnom",
        "expt",
        "falt",
        "fin2",
        "fin3",
        "fina",
        "frac",
        "fwid",
        "half",
        "haln",
        "halt",
        "hist",
        "hkna",
        "hlig",
        "hngl",
        "hwid",
        "init",
        "isol",
        "ital",
        "jalt",
        "jp78",
        "jp83",
        "jp90",
        "kern",
        "lfbd",
        "liga",
        "ljmo",
        "lnum",
        "locl",
        "mark",
        "med2",
        "medi",
        "mgrk",
        "mkmk",
        "mset",
        "nalt",
        "nlck",
        "nukt",
        "numr",
        "onum",
        "opbd",
        "ordn",
        "ornm",
        "palt",
        "pcap",
        "pnum",
        "pref",
        "pres",
        "pstf",
        "psts",
        "pwid",
        "qwid",
        "rand",
        "rlig",
        "rphf",
        "rtbd",
        "rtla",
        "ruby",
        "salt",
        "sinf",
        "size",
        "smcp",
        "smpl",
        "ss01",
        "ss02",
        "ss03",
        "ss04",
        "ss05",
        "ss06",
        "ss07",
        "ss08",
        "ss09",
        "ss10",
        "ss11",
        "ss12",
        "ss13",
        "ss14",
        "ss15",
        "ss16",
        "ss17",
        "ss18",
        "ss19",
        "ss20",
        "subs",
        "sups",
        "swsh",
        "titl",
        "tjmo",
        "tnam",
        "tnum",
        "trad",
        "twid",
        "unic",
        "valt",
        "vatu",
        "vert",
        "vhal",
        "vjmo",
        "vkna",
        "vkrn",
        "vpal",
        "vrt2",
        "zero"
    };
    
    private static String tagLabel(String tag)
    {
        StringBuffer result = new StringBuffer();
        String upperTag = tag.toUpperCase();
        
        for (int i = 0; i < upperTag.length(); i += 1) {
            char ch = upperTag.charAt(i);
            
            if ((ch < 'A' || ch > 'Z') && (ch < '0' || ch > '9')) {
                ch = '_';
            }
            
            result.append(ch);
        }
        
        return result.toString();
    }
    
    private static void dumpTags(String enumName, String[] tags)
    {
        System.out.println("enum LE" + enumName + "Tags {");
        
        for (int i = 0; i < tags.length; i += 1) {
            String tag = tags[i];
            
            System.out.println("    LE_" + tagLabel(tag) + "_" + enumName.toUpperCase() +
                "_TAG = " + TagUtilities.makeTag(tag) + "UL" +
                (i == tags.length - 1? " " : ",") + " /* '" + tag + "' */");
        }
        
        System.out.println("};");
    }
    
    public static void main(String[] args)
    {
        dumpTags("Table", tableTags);
        dumpTags("Feature", featureTags);
    }
}
