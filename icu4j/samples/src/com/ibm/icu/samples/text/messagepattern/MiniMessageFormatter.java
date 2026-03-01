// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
*******************************************************************************
*   Copyright (C) 2010-2011, International Business Machines
*   Corporation and others.  All Rights Reserved.
*******************************************************************************
*   created on: 2010aug21
*   created by: Markus W. Scherer
*/

package com.ibm.icu.samples.text.messagepattern;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.ibm.icu.text.MessagePattern;
import com.ibm.icu.text.MessagePattern.ArgType;
import com.ibm.icu.text.MessagePattern.Part;
import com.ibm.icu.util.Freezable;

/**
 * Mini message formatter for a small subset of the ICU MessageFormat syntax.
 * Supports only string substitution and select formatting.
 * @author Markus Scherer
 * @since 2010-aug-21
 */
public final class MiniMessageFormatter implements Freezable<MiniMessageFormatter> {
    public MiniMessageFormatter() {
        this.msg=new MessagePattern();
    }

    public MiniMessageFormatter(MessagePattern msg) {
        this.msg=(MessagePattern)msg.clone();
    }

    public MiniMessageFormatter(String msg) {
        this.msg=new MessagePattern(msg);
    }

    public MiniMessageFormatter applyPattern(String msg) {
        this.msg.parse(msg);
        return this;
    }

    public String getPatternString() {
        return msg.getPatternString();
    }

    public boolean hasNamedArguments() {
        return msg.hasNamedArguments();
    }

    public boolean hasNumberedArguments() {
        return msg.hasNumberedArguments();
    }

    /**
     * Formats the parsed message with positional arguments.
     * Supports only string substitution (e.g., {3}) and select format.
     * @param dest gets the formatted message appended
     * @param args positional arguments
     * @return dest
     */
    public Appendable format(Appendable dest, Object... args) {
        if(msg.hasNamedArguments()) {
            throw new IllegalArgumentException(
                "Formatting message with named arguments using positional argument values.");
        }
        format(0, dest, args, null);
        return dest;
    }

    public static final String format(String msg, Object... args) {
        return new MiniMessageFormatter(msg).format(new StringBuilder(2*msg.length()), args).toString();
    }

    public Appendable format(Appendable dest, Map<String, Object> argsMap) {
        if(msg.hasNumberedArguments()) {
            throw new IllegalArgumentException(
                "Formatting message with numbered arguments using named argument values.");
        }
        format(0, dest, null, argsMap);
        return dest;
    }

    public static final String format(String msg, Map<String, Object> argsMap) {
        return new MiniMessageFormatter(msg).format(new StringBuilder(2*msg.length()), argsMap).toString();
    }

    private int format(int msgStart, Appendable dest, Object[] args, Map<String, Object> argsMap) {
        try {
            String msgString=msg.getPatternString();
            int prevIndex=msg.getPart(msgStart).getLimit();
            for(int i=msgStart+1;; ++i) {
                Part part=msg.getPart(i);
                Part.Type type=part.getType();
                int index=part.getIndex();
                dest.append(msgString, prevIndex, index);
                if(type==Part.Type.MSG_LIMIT) {
                    return i;
                }
                if(type==Part.Type.SKIP_SYNTAX || type==Part.Type.INSERT_CHAR) {
                    prevIndex=part.getLimit();
                    continue;
                }
                assert type==Part.Type.ARG_START : "Unexpected Part "+part+" in parsed message.";
                int argLimit=msg.getLimitPartIndex(i);
                ArgType argType=part.getArgType();
                part=msg.getPart(++i);
                Object arg;
                if(args!=null) {
                    try {
                        arg=args[part.getValue()];  // args[ARG_NUMBER]
                    } catch(IndexOutOfBoundsException e) {
                        throw new IndexOutOfBoundsException(
                            "No argument at index "+part.getValue());
                    }
                } else {
                    arg=argsMap.get(msg.getSubstring(part));  // args[ARG_NAME]
                    if(arg==null) {
                        throw new IndexOutOfBoundsException(
                            "No argument for name "+msg.getSubstring(part));
                    }
                }
                String argValue=arg.toString();
                ++i;
                if(argType==ArgType.NONE) {
                    dest.append(argValue);
                } else if(argType==ArgType.SELECT) {
                    // Similar to SelectFormat.findSubMessage().
                    int subMsgStart=0;
                    for(;; ++i) {  // (ARG_SELECTOR, message) pairs until ARG_LIMIT
                        part=msg.getPart(i++);
                        if(part.getType()==Part.Type.ARG_LIMIT) {
                            assert subMsgStart!=0;  // The parser made sure this is the case.
                            break;
                        // else: part is an ARG_SELECTOR followed by a message
                        } else if(msg.partSubstringMatches(part, argValue)) {
                            // keyword matches
                            subMsgStart=i;
                            break;
                        } else if(subMsgStart==0 && msg.partSubstringMatches(part, "other")) {
                            subMsgStart=i;
                        }
                        i=msg.getLimitPartIndex(i);
                    }
                    format(subMsgStart, dest, args, argsMap);
                } else {
                    throw new UnsupportedOperationException("Unsupported argument type "+argType);
                }
                prevIndex=msg.getPart(argLimit).getLimit();
                i=argLimit;
            }
        } catch(IOException e) {  // Appendable throws IOException
            throw new RuntimeException(e);  // We do not want a throws clause.
        }
    }

    /**
     * Presents an array of (String, Object) pairs as a Map.
     * Only for temporary use for formatting with named arguments.
     */
    public static Map<String, Object> mapFromNameValuePairs(Object[] args) {
        HashMap<String, Object> argsMap = new HashMap<String, Object>();
        for(int i=0; i<args.length; i+=2) {
            argsMap.put((String)args[i], args[i+1]);
        }
        return argsMap;
    }

    public MiniMessageFormatter cloneAsThawed() {
        // TODO Auto-generated method stub
        return null;
    }

    public MiniMessageFormatter freeze() {
        msg.freeze();
        return this;
    }

    public boolean isFrozen() {
        return msg.isFrozen();
    }

    private final MessagePattern msg;
}
