package com.ibm.text.utility;

public class UTF16Plus {
    public static int charAt(StringBuffer source, int offset16) {
        return UTF32.char32At(source, offset16);
    }
}

