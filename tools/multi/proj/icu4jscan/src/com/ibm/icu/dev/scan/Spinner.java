// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/* Copyright (C) 2008-2012 IBM Corporation and Others. All Rights Reserved. */

package com.ibm.icu.dev.scan;

class Spinner {
    String str = "";
    int n = 0;
    int max = -1;
    Spinner() {
    }
    Spinner(String str) {
        this.str = str;
    }
    Spinner(String str, int max) {
        this.str = str;
        this.max = max;
    }
    void spin() {
        spin(null);
    }
    void spin(String what) {
        // TODO: time based.
        n++;
        if((n%25)==0) {
            String nstr="";
            if(max>0) {
                nstr="/"+max;
            }
            System.err.println("#"+str+".."+n+nstr+" "+(what!=null?what:""));
        }
    }
}