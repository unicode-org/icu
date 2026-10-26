// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 ***************************************************************************
 *   Copyright (C) 2002-2009 International Business Machines Corporation   *
 *   and others. All rights reserved.                                      *
 ***************************************************************************
 */
package com.ibm.icu.text;

import com.ibm.icu.lang.UCharacter;
import java.text.ParsePosition;
import java.util.HashMap;

class RBBISymbolTable implements SymbolTable {

    HashMap<String, RBBISymbolTableEntry> fHashTable;
    RBBIRuleScanner fRuleScanner;

    static class RBBISymbolTableEntry {
        String key;
        RBBINode val;
    }

    RBBISymbolTable(RBBIRuleScanner rs) {
        fRuleScanner = rs;
        fHashTable = new HashMap<String, RBBISymbolTableEntry>();
    }

    //
    //  RBBISymbolTable::lookup       This function from the abstract symbol table interface
    //                                looks up a variable name and returns a UnicodeString
    //                                containing the substitution text.
    //
    //                                The variable name does NOT include the leading $.
    //
    @Override
    public char[] lookup(String s) {
        final RBBISymbolTableEntry el = fHashTable.get(s);
        if (el == null) {
            return null;
        }

        final RBBINode exprNode = el.val.fLeftChild; // Root node of expression for variable
        // Return the original source string for the expression.
        // Note that for set-valued variables used in UnicodeSet expressions, this would be rejected
        // by the UnicodeSet parser if the source itself contains variable references.  For
        // instance, with
        //     $CaseIgnorable   = [[:Mn:][:Me:][:Cf:][:Lm:][:Sk:] \u0027 \u00AD \u2019];
        //     $Cased = [[:Upper_Case:][:Lower_Case:][:Lt:] - $CaseIgnorable];
        // If lookupSet were not overridden, when parsing the right-hand side of
        //     $NotCased        = [[^ $Cased] - $CaseIgnorable];
        // there would be a call to lookup("Cased") which would return
        //     "[[:Upper_Case:][:Lower_Case:][:Lt:]-$CaseIgnorable]". This contains a variable,
        // which is  disallowed by the UnicodeSet parser inside a variable expansion.
        // However, set-valued variables are pre-parsed, and returned by lookupSet instead, so this
        // call to lookup() never happens; instead, lookupSet("CaseIgnorable") is called when
        // computing $Cased and returns the non-null value of $CaseIgnorable, and then when
        // computing $NotCased, lookupSet("Cased") returns the value computed for $Cased.
        return exprNode.fText.toCharArray();
    }

    @Override
    public UnicodeSet lookupSet(String s) {
        final RBBISymbolTableEntry el = fHashTable.get(s);
        if (el == null) {
            return null;
        }
        final RBBINode exprNode = el.val.fLeftChild;
        if (exprNode.fType == RBBINode.setRef) {
            return exprNode.fLeftChild.fInputSet;
        } else {
            return null;
        }
    }

    // No longer used, see ICU-23297.
    @Override
    public UnicodeMatcher lookupMatcher(int ch) {
        return null;
    }

    //
    // RBBISymbolTable::parseReference   This function from the abstract symbol table interface
    //                                   looks for a $variable name in the source text.
    //                                   It does not look it up, only scans for it.
    //                                   It is used by the UnicodeSet parser.
    //
    @Override
    public String parseReference(String text, ParsePosition pos, int limit) {
        int start = pos.getIndex();
        int i = start;
        String result = "";
        while (i < limit) {
            int c = UTF16.charAt(text, i);
            if ((i == start && !UCharacter.isUnicodeIdentifierStart(c))
                    || !UCharacter.isUnicodeIdentifierPart(c)) {
                break;
            }
            i += UTF16.getCharCount(c);
        }
        if (i == start) { // No valid name chars
            return result; // Indicate failure with empty string
        }
        pos.setIndex(i);
        result = text.substring(start, i);
        return result;
    }

    //
    // RBBISymbolTable::lookupNode      Given a key (a variable name), return the
    //                                  corresponding RBBI Node.  If there is no entry
    //                                  in the table for this name, return NULL.
    //
    RBBINode lookupNode(String key) {

        RBBINode retNode = null;
        RBBISymbolTableEntry el;

        el = fHashTable.get(key);
        if (el != null) {
            retNode = el.val;
        }
        return retNode;
    }

    //
    //    RBBISymbolTable::addEntry     Add a new entry to the symbol table.
    //                                  Indicate an error if the name already exists -
    //                                    this will only occur in the case of duplicate
    //                                    variable assignments.
    //
    void addEntry(String key, RBBINode val) {
        RBBISymbolTableEntry e;
        e = fHashTable.get(key);
        if (e != null) {
            fRuleScanner.error(RBBIRuleBuilder.U_BRK_VARIABLE_REDFINITION);
            return;
        }

        e = new RBBISymbolTableEntry();
        e.key = key;
        e.val = val;
        fHashTable.put(e.key, e);
    }

    //
    //  RBBISymbolTable::print    Debugging function, dump out the symbol table contents.
    //
    void rbbiSymtablePrint() {
        System.out.print(
                "Variable Definitions\n"
                        + "Name               Node Val     String Val\n"
                        + "----------------------------------------------------------------------\n");

        RBBISymbolTableEntry[] syms = fHashTable.values().toArray(new RBBISymbolTableEntry[0]);

        for (int i = 0; i < syms.length; i++) {
            RBBISymbolTableEntry s = syms[i];

            System.out.print("  " + s.key + "  "); // TODO:  format output into columns.
            System.out.print("  " + s.val + "  ");
            System.out.print(s.val.fLeftChild.fText);
            System.out.print("\n");
        }

        System.out.println("\nParsed Variable Definitions\n");
        for (int i = 0; i < syms.length; i++) {
            RBBISymbolTableEntry s = syms[i];
            System.out.print(s.key);
            s.val.fLeftChild.printTree(true);
            System.out.print("\n");
        }
    }
}
