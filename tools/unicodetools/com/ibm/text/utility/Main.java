/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/utility/Main.java,v $
* $Date: 2002/06/22 21:01:25 $
* $Revision: 1.1 $
*
*******************************************************************************
*/

package com.ibm.text.utility;

import java.util.*;
import java.text.*;
import java.io.*;

public class Main {
    
    static public class CollatorStyle extends EnumBase {
        public static CollatorStyle
            ZEROED = (CollatorStyle) makeNext(new CollatorStyle(), "ZEROED"),
            SHIFTED = (CollatorStyle) makeNext(new CollatorStyle(), "SHIFTED"),
            NON_IGNORABLE = (CollatorStyle) makeNext(new CollatorStyle(), "NON_IGNORABLE");
        public CollatorStyle next() { return (CollatorStyle) internalNext(); }
        private CollatorStyle() {}
    }
    
    static public class NormalizerType extends EnumBase {
        public static NormalizerType
            NFC = (NormalizerType) makeNext(new NormalizerType(), "NFC"),
            NFD = (NormalizerType) makeNext(new NormalizerType(), "NFD"),
            NFKC = (NormalizerType) makeNext(new NormalizerType(), "NFKC"),
            NFKD = (NormalizerType) makeNext(new NormalizerType(), "NFKD");
        public NormalizerType next() { return (NormalizerType) internalNext(); }
        private NormalizerType() {}
    }
    
    static public class Length extends EnumBase {
        public static Length
            SHORT = (Length) makeNext(new Length(), "SHORT"),
            NORMAL = (Length) makeNext(new Length(), "NORMAL"),
            LONG = (Length) makeNext(new Length(), "LONG");
        public Length next() { return (Length) internalNext(); }
        private Length() {}
    }
    
    static public void main (String[] args) {
        for (CollatorStyle i = CollatorStyle.ZEROED; i != null; i = i.next()) {
            System.out.println(i);
        }
        for (NormalizerType i = NormalizerType.NFC; i != null; i = i.next()) {
            System.out.println(i);
        }
        
        NormalizerType foo = new NormalizerType();
    }
    
}