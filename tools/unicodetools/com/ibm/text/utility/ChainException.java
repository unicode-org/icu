package com.ibm.text.utility;


import java.text.*;
import java.io.*;
public class ChainException extends RuntimeException {
    Object[] keyData;
    String messageFormat;
    Exception chain;

    public ChainException (String messageFormat, Object[] objects) {
        this.messageFormat = messageFormat;
        keyData = (Object[]) objects.clone();
    }

    public ChainException (String messageFormat, Object[] objects, Exception chainedException) {
        this.messageFormat = messageFormat;
        keyData = objects == null ? null : (Object[]) objects.clone();
        chain = chainedException;
    }

    public String getMessage() {
        String chainMsg = "";
        if (chain != null) {
            chainMsg = "; " + chain.getClass().getName()
                + ", " + chain.getMessage();
            StringWriter w = new StringWriter();
            PrintWriter p = new PrintWriter(w);
            chain.printStackTrace(p);
            chainMsg += ", " + w.getBuffer();
            p.close();
        }
        String main = "";
        if (keyData != null) main = MessageFormat.format(messageFormat, keyData);
        return main + chainMsg;
    }
}

