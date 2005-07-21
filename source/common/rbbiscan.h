//
//  rbbiscan.h
//
//  Copyright (C) 2002-2003, International Business Machines Corporation and others.
//  All Rights Reserved.
//
//  This file contains declarations for class RBBIRuleScanner
//


#ifndef RBBISCAN_H
#define RBBISCAN_H

#include "unicode/utypes.h"
#include "unicode/uobject.h"
#include "unicode/rbbi.h"
#include "unicode/uniset.h"
#include "unicode/parseerr.h"
#include "uhash.h"
#include "uvector.h"
#include "unicode/symtable.h"// For UnicodeSet parsing, is the interface that
                          //    looks up references to $variables within a set.
#include "rbbinode.h"
//#include "rbbitblb.h"



U_NAMESPACE_BEGIN

class   RBBIRuleBuilder;
class   RBBISymbolTable;


//--------------------------------------------------------------------------------
//
//  class RBBIRuleScanner does the lowest level, character-at-a-time
//                        scanning of break iterator rules.  
//
//                        The output of the scanner is parse trees for
//                        the rule expressions and a list of all Unicode Sets
//                        encountered.
//
//--------------------------------------------------------------------------------
static const int    kStackSize = 100;               // The size of the state stack for
                                                    //   rules parsing.  Corresponds roughly
                                                    //   to the depth of parentheses nesting
                                                    //   that is allowed in the rules.

enum EParseAction {dummy01, dummy02};               // Placeholder enum for the specifier for
                                                    //   actions that are specified in the
                                                    //   rule parsing state table.

class RBBIRuleScanner : public UMemory {
public:

    struct RBBIRuleChar {
        UChar32             fChar;
        UBool               fEscaped;
    };

    RBBIRuleScanner(RBBIRuleBuilder  *rb);


    virtual    ~RBBIRuleScanner();

    void        nextChar(RBBIRuleChar &c);          // Get the next char from the input stream.
                                                    // Return false if at end.

    UBool       push(const RBBIRuleChar &c);        // Push (unget) one character.
                                                    //   Only a single character may be pushed.

    void        parse();                            // Parse the rules, generating two parse
                                                    //   trees, one each for the forward and
                                                    //   reverse rules,
                                                    //   and a list of UnicodeSets encountered.

    /**
     * Return a rules string without unnecessary
     * characters.
     */
    static UnicodeString stripRules(const UnicodeString &rules);
private:

    UBool       doParseActions(EParseAction a);
    void        error(UErrorCode e);                   // error reporting convenience function.
    void        fixOpStack(RBBINode::OpPrecedence p);
                                                       //   a character.
    void        findSetFor(const UnicodeString &s, RBBINode *node, UnicodeSet *setToAdopt = NULL);

    UChar32     nextCharLL();
    void        printNodeStack(const char *title);
    RBBINode    *pushNewNode(RBBINode::NodeType  t);
    void        scanSet();


    RBBIRuleBuilder               *fRB;              // The rule builder that we are part of.

    int32_t                       fScanIndex;        // Index of current character being processed
                                                     //   in the rule input string.
    int32_t                       fNextIndex;        // Index of the next character, which
                                                     //   is the first character not yet scanned.
    UBool                         fQuoteMode;        // Scan is in a 'quoted region'
    int                           fLineNum;          // Line number in input file.
    int                           fCharNum;          // Char position within the line.
    UChar32                       fLastChar;         // Previous char, needed to count CR-LF
                                                     //   as a single line, not two.

    RBBIRuleChar                  fC;                // Current char for parse state machine
                                                     //   processing.
    UnicodeString                 fVarName;          // $variableName, valid when we've just
                                                     //   scanned one.

    RBBIRuleTableEl               **fStateTable;     // State Transition Table for RBBI Rule
                                                     //   parsing.  index by p[state][char-class]

    uint16_t                      fStack[kStackSize];  // State stack, holds state pushes
    int                           fStackPtr;           //  and pops as specified in the state
                                                       //  transition rules.

    RBBINode                      *fNodeStack[kStackSize]; // Node stack, holds nodes created
                                                           //  during the parse of a rule
    int                            fNodeStackPtr;


    UBool                          fReverseRule;     // True if the rule currently being scanned
                                                     //  is a reverse direction rule (if it
                                                     //  starts with a '!')

    UBool                          fLookAheadRule;   // True if the rule includes a '/'
                                                     //   somewhere within it.

    RBBISymbolTable               *fSymbolTable;     // symbol table, holds definitions of
                                                     //   $variable symbols.

    UHashtable                    *fSetTable;        // UnicocodeSet hash table, holds indexes to
                                                     //   the sets created while parsing rules.
                                                     //   The key is the string used for creating
                                                     //   the set.

    UnicodeSet                    *fRuleSets[10];    // Unicode Sets that are needed during
                                                     //  the scanning of RBBI rules.  The
                                                     //  indicies for these are assigned by the
                                                     //  perl script that builds the state tables.
                                                     //  See rbbirpt.h.

    int32_t                        fRuleNum;         // Counts each rule as it is scanned.

    int32_t                        fOptionStart;     // Input index of start of a !!option
                                                     //   keyword, while being scanned.

    UnicodeSet *gRuleSet_rule_char;
    UnicodeSet *gRuleSet_white_space;
    UnicodeSet *gRuleSet_name_char;
    UnicodeSet *gRuleSet_name_start_char;

    RBBIRuleScanner(const RBBIRuleScanner &other); // forbid copying of this class
    RBBIRuleScanner &operator=(const RBBIRuleScanner &other); // forbid copying of this class
};

U_NAMESPACE_END

#endif
