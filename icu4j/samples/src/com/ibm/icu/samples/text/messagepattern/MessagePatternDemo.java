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

import com.ibm.icu.text.MessagePattern;

/**
 * Demo code for MessagePattern class.
 * Pretty-prints the list of MessagePattern Parts and uses the MiniMessageFormatter
 * with a few patterns.
 * @author Markus Scherer
 * @since 2010-aug-21
 */
public final class MessagePatternDemo {
    private static final String manySpaces="                    ";

    private static final void printParts(MessagePattern msg) {
        String autoQA=msg.autoQuoteApostropheDeep();
        if(!autoQA.equals(msg.getPatternString())) {
            System.out.println("autoQA:   "+autoQA);
        }
        String indent="";
        StringBuilder explanation=new StringBuilder();
        MessagePattern.Part prevPart=null;
        int count=msg.countParts();
        for(int i=0; i<count; ++i) {
            explanation.delete(0, 0x7fffffff);
            MessagePattern.Part part=msg.getPart(i);
            assert prevPart==null || prevPart.getLimit()<=part.getIndex();
            String partString=part.toString();
            MessagePattern.Part.Type type=part.getType();
            if(type==MessagePattern.Part.Type.MSG_START) {
                indent=manySpaces.substring(0, part.getValue()*2);
            }
            if(part.getLength()>0) {
                explanation.append("=\"").append(msg.getSubstring(part)).append('"');
            }
            if(type.hasNumericValue()) {
                explanation.append('=').append(msg.getNumericValue(part));
            }
            System.out.format("%2d: %s%s%s\n", i, indent, partString, explanation);
            if(type==MessagePattern.Part.Type.MSG_LIMIT) {
                int nestingLevel=part.getValue();
                if(nestingLevel>1) {
                    indent=manySpaces.substring(0, (nestingLevel-1)*2);  // outdent
                } else {
                    indent="";
                }
            }
            prevPart=part;
        }
    }

    private static final MessagePattern print(String s) {
        System.out.println("message:  "+s);
        try {
            MessagePattern msg=new MessagePattern(s);
            printParts(msg);
            return msg;
        } catch(Exception e) {
            System.out.println("Exception: "+e.getMessage());
            return null;
        }
    }

    private static final void printFormat(String s, Object... args) {
        MessagePattern msg=print(s);
        if(msg!=null) {
            System.out.println(new MiniMessageFormatter(msg).format(new StringBuilder(), args));
        }
    }

    private static final void printFormatWithNamedArgs(String s, Object... args) {
        MessagePattern msg=print(s);
        if(msg!=null) {
            System.out.println(new MiniMessageFormatter(msg).format(
                new StringBuilder(), MiniMessageFormatter.mapFromNameValuePairs(args)));
        }
    }

    public static void main(String[] argv) {
        print("Hello!");
        print("Hel'lo!");
        print("Hel'{o");
        print("Hel'{'o");
        // double apostrophe inside quoted literal text still encodes a single apostrophe
        printFormat("a'{bc''de'f");
        print("a'{bc''de'f{0,number,g'hi''jk'l#}");
        print("abc{0}def");
        print("abc{ arg }def");
        print("abc{1}def{arg}ghi");
        print("abc{2, number}ghi{3, select, xx {xxx} other {ooo}} xyz");
        print("abc{gender,select,"+
                  "other{His name is {person,XML,<entry name=\"PERSON\">{$PERSON}</entry>}.}}xyz");
        print("abc{num_people, plural, offset:17 few{fff} other {oooo}}xyz");
        print("abc{ num , plural , offset: 2 =1 {1} =-1 {-1} =3.14 {3.14} other {oo} }xyz");
        print("I don't {a,plural,other{w'{'on't #'#'}} and "+
              "{b,select,other{shan't'}'}} '{'''know'''}' and "+
              "{c,choice,0#can't'|'}"+
              "{z,number,#'#'###.00'}'}.");
        print("a_{0,choice,-∞ #-inf|  5≤ five | 99 # ninety'|'nine  }_z");
        print("a_{0,plural,other{num=#'#'=#'#'={1,number,##}!}}_z");
        print("}}}{0}}");  // yes, unmatched '}' are ok in ICU MessageFormat
        printFormat("Hello {0}!", "Alice");
        String msg="++{0, select, female{{1} calls you her friend}"+
                                 "other{{1} calls you '{their}' friend}"+
                                 "male{{1} calls you his friend}}--";
        printFormat(msg, "female", "Alice");
        printFormat(msg, "male", "Bob");
        printFormat(msg, "unknown", "sushifan3");
        msg="_'__{gender, select, female{Her n'ame is {person_name}.}"+
                                "other{His n'ame is {person_name}.}}__'_";
        printFormatWithNamedArgs(msg, "gender", "female", "person_name", "Alice");
        printFormatWithNamedArgs(msg, "gender", "male", "person_name", "Bob");
    }
}
