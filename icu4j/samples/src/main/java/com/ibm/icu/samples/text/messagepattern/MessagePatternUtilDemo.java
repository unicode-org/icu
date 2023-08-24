// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
*******************************************************************************
*   Copyright (C) 2011, International Business Machines
*   Corporation and others.  All Rights Reserved.
*******************************************************************************
*   created on: 2011jul14
*   created by: Markus W. Scherer
*/

package com.ibm.icu.samples.text.messagepattern;

import java.util.ArrayList;
import java.util.List;

import com.ibm.icu.text.MessagePattern;
import com.ibm.icu.text.MessagePatternUtil;
import com.ibm.icu.text.MessagePatternUtil.VariantNode;

/**
 * Demo code for MessagePattern class.
 * @author Markus Scherer
 * @since 2011-jul-14
 */
public class MessagePatternUtilDemo {
    private static final String manySpaces="                    ";

    private static final void printMessage(MessagePatternUtil.MessageNode msg, int depth) {
        String indent = manySpaces.substring(0, depth * 2);
        for (MessagePatternUtil.MessageContentsNode contents : msg.getContents()) {
            switch (contents.getType()) {
            case TEXT:
                System.out.println(indent + "text: «" +
                                   ((MessagePatternUtil.TextNode)contents).getText() + "»");
                break;
            case ARG:
                printArg((MessagePatternUtil.ArgNode)contents, depth);
                break;
            case REPLACE_NUMBER:
                System.out.println(indent + "replace: number");
                break;
            }
        }
    }

    private static final void printArg(MessagePatternUtil.ArgNode arg, int depth) {
        System.out.print(manySpaces.substring(0, depth * 2) + "arg: «" + arg.getName() + "»");
        MessagePattern.ArgType argType = arg.getArgType();
        if (argType == MessagePattern.ArgType.NONE) {
            System.out.println(" (no type)");
        } else {
            System.out.print(" (" + arg.getTypeName() + ")");
            if (argType == MessagePattern.ArgType.SIMPLE) {
                String styleString = arg.getSimpleStyle();
                if (styleString == null) {
                    System.out.println(" (no style)");
                } else {
                    System.out.println(" style: «" + styleString + "»");
                }
            } else {
                System.out.println();
                printComplexArgStyle(arg.getComplexStyle(), depth + 1);
            }
        }
    }

    private static final void printComplexArgStyle(MessagePatternUtil.ComplexArgStyleNode style,
                                                   int depth) {
        if (style.hasExplicitOffset()) {
            System.out.println(manySpaces.substring(0, depth * 2) + "offset: " + style.getOffset());
        }
        String indent = manySpaces.substring(0, depth * 2);
        MessagePattern.ArgType argType = style.getArgType();
        for (MessagePatternUtil.VariantNode variant : style.getVariants()) {
            double value;
            switch (argType) {
            case CHOICE:
                System.out.println(indent + variant.getSelectorValue() + " " +
                                   variant.getSelector() + ":");
                break;
            case PLURAL:
                value = variant.getSelectorValue();
                if (value == MessagePattern.NO_NUMERIC_VALUE) {
                    System.out.println(indent + variant.getSelector() + ":");
                } else {
                    System.out.println(indent + variant.getSelector() + " (" + value + "):");
                }
                break;
            case SELECT:
                System.out.println(indent + variant.getSelector() + ":");
                break;
            }
            printMessage(variant.getMessage(), depth + 1);
        }
    }

    /**
     * This is a <em>prototype/demo/sample</em> for how we could use the MessagePatternUtil class
     * for generating something like JavaScript code for evaluating some
     * of the MessageFormat syntax.
     *
     * <p>This is not intended to be production code, nor to generate production code
     * or even syntactically correct JavaScript.
     * @param msg
     */
    private static final void genCode(MessagePatternUtil.MessageNode msg) {
        List<String> args = new ArrayList<String>();
        addArgs(msg, args);
        System.out.print("def function(");
        boolean firstArg = true;
        for (String argName : args) {
            if (firstArg) {
                System.out.print(argName);
                firstArg = false;
            } else {
                System.out.print(", " + argName);
            }
        }
        System.out.println(") {");
        genCode(msg, 1, true, "");
        System.out.println("  return result");
        System.out.println("}");
    }

    private static final void genCode(MessagePatternUtil.MessageNode msg,
                                      int depth,
                                      boolean firstResult,
                                      String pluralNumber) {
        String prefix = manySpaces.substring(0, depth * 2) + "result ";
        for (MessagePatternUtil.MessageContentsNode contents : msg.getContents()) {
            String operator = firstResult ? "=" : "+=";
            switch (contents.getType()) {
            case TEXT:
                System.out.println(
                        prefix + operator + " \"" +
                        escapeString(((MessagePatternUtil.TextNode)contents).getText()) +
                "\"");
                break;
            case ARG:
                genCode((MessagePatternUtil.ArgNode)contents, depth, firstResult);
                break;
            case REPLACE_NUMBER:
                System.out.println(prefix + operator + " formatNumber(" + pluralNumber + ")");
                break;
            }
            firstResult = false;
        }
    }

    private static final void genCode(MessagePatternUtil.ArgNode arg,
                                      int depth,
                                      boolean firstResult) {
        String prefix = manySpaces.substring(0, depth * 2) + "result ";
        String operator = firstResult ? "=" : "+=";
        String argName = arg.getName();
        if (arg.getNumber() >= 0) {
            argName = "arg_" + argName;  // Prefix for numbered argument.
        }
        switch (arg.getArgType()) {
        case NONE:
            System.out.println(prefix + operator + " " + argName);
            break;
        case SIMPLE:
        case CHOICE:
            System.out.println(prefix + operator + " \"(unsupported syntax)\"");
            break;
        case PLURAL:
            genCodeForPlural(arg.getComplexStyle(), depth, firstResult, argName);
            break;
        case SELECT:
            genCodeForSelect(arg.getComplexStyle(), depth, firstResult, argName);
            break;
        }
    }

    private static final void genCodeForPlural(MessagePatternUtil.ComplexArgStyleNode style,
                                               int depth,
                                               boolean firstResult,
                                               String argName) {
        List<MessagePatternUtil.VariantNode> numericVariants =
            new ArrayList<MessagePatternUtil.VariantNode>();
        List<MessagePatternUtil.VariantNode> keywordVariants =
            new ArrayList<MessagePatternUtil.VariantNode>();
        MessagePatternUtil.VariantNode otherVariant =
            style.getVariantsByType(numericVariants, keywordVariants);
        double offset = style.getOffset();
        String pluralNumber = offset == 0. ? argName : argName + " - " + offset;
        int origDepth = depth;
        if (!numericVariants.isEmpty()) {
            genCodeForNumericVariants(numericVariants, depth++, firstResult, argName, pluralNumber);
        }
        if (!keywordVariants.isEmpty()) {
            System.out.println(manySpaces.substring(0, depth * 2) +
                               "_keyword = PluralRules.select(" + pluralNumber + ")");
            genCodeForKeywordVariants(keywordVariants, depth++, firstResult,
                                      "_keyword", pluralNumber);
        }
        genCode(otherVariant.getMessage(), depth, firstResult, pluralNumber);
        if (origDepth < depth) {
            System.out.println(manySpaces.substring(0, --depth * 2) + "}");
            if (origDepth < depth) {
                System.out.println(manySpaces.substring(0, --depth * 2) + "}");
            }
        }
    }

    private static final void genCodeForSelect(MessagePatternUtil.ComplexArgStyleNode style,
                                               int depth,
                                               boolean firstResult,
                                               String argName) {
        List<MessagePatternUtil.VariantNode> keywordVariants =
            new ArrayList<MessagePatternUtil.VariantNode>();
        MessagePatternUtil.VariantNode otherVariant = style.getVariantsByType(null, keywordVariants);
        if (keywordVariants.isEmpty()) {
            genCode(otherVariant.getMessage(), depth, firstResult, "");
        } else {
            genCodeForKeywordVariants(keywordVariants, depth, firstResult, argName, "");
            genCode(otherVariant.getMessage(), depth + 1, firstResult, "");
            System.out.println(manySpaces.substring(0, depth * 2) + "}");
        }
    }

    private static final void genCodeForNumericVariants(List<VariantNode> variants,
                                                        int depth,
                                                        boolean firstResult,
                                                        String varName,
                                                        String pluralNumber) {
        String indent = manySpaces.substring(0, depth++ * 2);
        boolean firstVariant = true;
        for (MessagePatternUtil.VariantNode variant : variants) {
            System.out.println(
                    indent +
                    (firstVariant ? "if (" : "} else if (") +
                    varName + " == " + variant.getSelectorValue() + ") {");
            genCode(variant.getMessage(), depth, firstResult, pluralNumber);
            firstVariant = false;
        }
        System.out.println(indent + "} else {");
    }

    private static final void genCodeForKeywordVariants(List<VariantNode> variants,
                                                        int depth,
                                                        boolean firstResult,
                                                        String varName,
                                                        String pluralNumber) {
        String indent = manySpaces.substring(0, depth++ * 2);
        boolean firstVariant = true;
        for (MessagePatternUtil.VariantNode variant : variants) {
            System.out.println(
                    indent +
                    (firstVariant ? "if (" : "} else if (") +
                    varName + " == \"" + variant.getSelector() + "\") {");
            genCode(variant.getMessage(), depth, firstResult, pluralNumber);
            firstVariant = false;
        }
        System.out.println(indent + "} else {");
    }

    /**
     * Adds the message's argument names to the args list.
     * Adds each argument only once, in the order of first appearance.
     * Numbered arguments get an "arg_" prefix prepended.
     * @param msg
     * @param args
     */
    private static final void addArgs(MessagePatternUtil.MessageNode msg, List<String> args) {
        for (MessagePatternUtil.MessageContentsNode contents : msg.getContents()) {
            if (contents.getType() == MessagePatternUtil.MessageContentsNode.Type.ARG) {
                MessagePatternUtil.ArgNode arg = (MessagePatternUtil.ArgNode)contents;
                String argName;
                if (arg.getNumber() >= 0) {
                    argName = "arg_" + arg.getNumber();  // Prefix for numbered argument.
                } else {
                    argName = arg.getName();
                }
                if (!args.contains(argName)) {
                    args.add(argName);
                }
                MessagePatternUtil.ComplexArgStyleNode complexStyle = arg.getComplexStyle();
                if (complexStyle != null) {
                    for (MessagePatternUtil.VariantNode variant : complexStyle.getVariants()) {
                        addArgs(variant.getMessage(), args);
                    }
                }
            }
        }
    }

    private static final String escapeString(String s) {
        if (s.indexOf('"') < 0) {
            return s;
        } else {
            return s.replace("\"", "\\\"");
        }
    }

    private static final MessagePatternUtil.MessageNode print(String s) {
        System.out.println("message:  «" + s + "»");
        try {
            MessagePatternUtil.MessageNode msg = MessagePatternUtil.buildMessageNode(s);
            printMessage(msg, 1);
            genCode(msg);
            return msg;
        } catch(Exception e) {
            System.out.println("Exception: "+e.getMessage());
            return null;
        }
    }

    public static void main(String[] argv) {
        print("Hello!");
        print("Hel'lo!");
        print("Hel'{o");
        print("Hel'{'o");
        // double apostrophe inside quoted literal text still encodes a single apostrophe
        print("a'{bc''de'f");
        print("a'{bc''de'f{0,number,g'hi''jk'l#}");
        print("abc{0}def");
        print("abc{ arg }def");
        print("abc{1}def{arg}ghi");
        print("abc{2, number}ghi{3, select, xx {xxx} other {ooo}} xyz");
        print("abc{gender,select,"+
                  "other{His name is {tc,XMB,<ph name=\"PERSON\">{$PERSON}</ph>}.}}xyz");
        print("abc{num_people, plural, offset:17 few{fff} other {oooo}}xyz");
        print("abc{ num , plural , offset: 2 =1 {1} =-1 {-1} =3.14 {3.14} other {oo} }xyz");
        print("I don't {a,plural,other{w'{'on't #'#'}} and "+
              "{b,select,other{shan't'}'}} '{'''know'''}' and "+
              "{c,choice,0#can't'|'}"+
              "{z,number,#'#'###.00'}'}.");
        print("a_{0,choice,-∞ #-inf|  5≤ five | 99 # ninety'|'nine  }_z");
        print("a_{0,plural,other{num=#'#'=#'#'={1,number,##}!}}_z");
        print("}}}{0}}");  // yes, unmatched '}' are ok in ICU MessageFormat
        print("Hello {0}!");
        String msg="++{0, select, female{{1} calls you her friend}"+
                                 "other{{1} calls you '{their}' friend}"+
                                 "male{{1} calls you his friend}}--";
        print(msg);
        msg="_'__{gender, select, female{Her n'ame is {person_name}.}"+
                                 "other{His n'ame is {person_name}.}}__'_";
        print(msg);
        print("{num,plural,offset:1 " +
                "=0{no one} =1{one, that is one and # others} " +
                "one{one and # (probably 1) others} few{one and # others} " +
                "other{lots & lots}}");
        print(
            "{p1_gender,select," +
              "female{" +
                "{p2_gender,select," +
                  "female{" +
                    "{num_people,plural,offset:1 "+
                      "=0{she alone}" +
                      "=1{she and her girlfriend {p2}}" +
                      "=2{she and her girlfriend {p2} and another}" +
                      "other{she, her girlfriend {p2} and # others}}}" +
                  "male{" +
                    "{num_people,plural,offset:1 "+
                      "=0{she alone}" +
                      "=1{she and her boyfriend {p2}}" +
                      "=2{she and her boyfriend {p2} and another}" +
                      "other{she, her boyfriend {p2} and # others}}}" +
                  "other{" +
                    "{num_people,plural,offset:1 "+
                      "=0{she alone}" +
                      "=1{she and her friend {p2}}" +
                      "=2{she and her friend {p2} and another}" +
                      "other{she, her friend {p2} and # others}}}}}" +
              "male{" +
                "{p2_gender,select," +
                  "female{" +
                    "{num_people,plural,offset:1 "+
                      "=0{he alone}" +
                      "=1{he and his girlfriend {p2}}" +
                      "=2{he and his girlfriend {p2} and another}" +
                      "other{he, his girlfriend {p2} and # others}}}" +
                    "male{" +
                      "{num_people,plural,offset:1 "+
                        "=0{he alone}" +
                        "=1{he and his boyfriend {p2}}" +
                        "=2{he and his boyfriend {p2} and another}" +
                        "other{he, his boyfriend {p2} and # others}}}" +
                    "other{" +
                      "{num_people,plural,offset:1 "+
                        "=0{she alone}" +
                        "=1{she and his friend {p2}}" +
                        "=2{she and his friend {p2} and another}" +
                        "other{she, his friend {p2} and # others}}}}}" +
              "other{" +
                "{p2_gender,select," +
                  "female{" +
                    "{num_people,plural,offset:1 "+
                      "=0{they alone}" +
                      "=1{they and their girlfriend {p2}}" +
                      "=2{they and their girlfriend {p2} and another}" +
                      "other{they, their girlfriend {p2} and # others}}}" +
                  "male{" +
                    "{num_people,plural,offset:1 "+
                      "=0{they alone}" +
                      "=1{they and their boyfriend {p2}}" +
                      "=2{they and their boyfriend {p2} and another}" +
                      "other{they, their boyfriend {p2} and # others}}}" +
                  "other{" +
                    "{num_people,plural,offset:1 "+
                      "=0{they alone}" +
                      "=1{they and their friend {p2}}" +
                      "=2{they and their friend {p2} and another}" +
                      "other{they, their friend {p2} and # others}}}}}}");
    }
}
