// 
//   Copyright (C) 2002 International Business Machines Corporation 
//   and others. All rights reserved.  
//
//   file:  regeximp.h
//
//           ICU Regular Expressions,
//               Definitions of constant values used in the compiled form of
//               a regular expression pattern.
//

#ifndef _REGEXIMP_H
#define _REGEXIMP_H

U_NAMESPACE_BEGIN

//
//  debugging support.  Enable one or more of the #defines immediately following
//
//#define REGEX_SCAN_DEBUG
#define REGEX_DUMP_DEBUG
#define REGEX_RUN_DEBUG
//  End of #defines inteded to be directly set.

#ifdef REGEX_SCAN_DEBUG
#define REGEX_SCAN_DEBUG_PRINTF printf
#else
#define REGEX_SCAN_DEBUG_PRINTF
#endif

#ifdef REGEX_DUMP_DEBUG
#define REGEX_DUMP_DEBUG_PRINTF printf
#else
#define REGEX_DUMP_DEBUG_PRINTF
#endif

#ifdef REGEX_RUN_DEBUG
#define REGEX_RUN_DEBUG_PRINTF printf
#define REGEX_DUMP_DEBUG_PRINTF printf
#else
#define REGEX_RUN_DEBUG_PRINTF
#endif

#if defined(REGEX_SCAN_DEBUG) || defined(REGEX_RUN_DEBUG) || defined(REGEX_DUMP_DEBUG)
#include <stdio.h>
#endif


//
//  Opcode types     In the compiled form of the regexp, these are the type, or opcodes,
//                   of the entries.
//
enum {
     URX_RESERVED_OP   = 0,    // For multi-operand ops, most non-first words.
     URX_BACKTRACK     = 1,
     URX_END           = 2,
     URX_ONECHAR       = 3,    // Value field is the 21 bit unicode char to match
     URX_STRING        = 4,    // Value field is index of string start
     URX_STRING_LEN    = 5,    // Value field is string length (code units)
     URX_STATE_SAVE    = 6,    // Value field is pattern position to push
     URX_NOP           = 7,
     URX_START_CAPTURE = 8,    // Value field is capture group number.
     URX_END_CAPTURE   = 9,    // Value field is capture group number
     URX_STATIC_SETREF = 10,   // Value field is index of set in array of sets.   
     URX_SETREF        = 11,   // Value field is index of set in array of sets.
     URX_DOTANY        = 12, 
     URX_JMP           = 13,   // Value field is destination position in
                                                    //   the pattern.
     URX_FAIL          = 14,   // Stop match operation,  No match.

     URX_BACKSLASH_A   = 15,   
     URX_BACKSLASH_B   = 16,   // Value field:  0:  \b    1:  \B
     URX_BACKSLASH_G   = 17, 
     URX_BACKSLASH_W   = 18,   // Value field:  0:  \w    1:  \W
     URX_BACKSLASH_X   = 19,
     URX_BACKSLASH_Z   = 20,   // \z   Unconditional end of line.

     URX_DOTANY_ALL    = 21,   // ., in the . matches any mode.
     URX_BACKSLASH_D   = 22,   // Value field:  0:  \d    1:  \D
     URX_CARET         = 23,   // Value field:  1:  multi-line mode.
     URX_DOLLAR        = 24,  // Also for \Z

     URX_CTR_INIT      = 25,   // Counter Inits for {Interval} loops.
     URX_CTR_INIT_NG   = 26,   //   3 kinds, normal, non-greedy, and possesive.
     URX_CTR_INIT_P    = 27,   //   These are 4 word opcodes.  See description.
     URX_CTR_LOOP      = 28,   // Loop Ops for {interval} loops.
     URX_CTR_LOOP_NG   = 29,   //   Also in three flavors.
     URX_CTR_LOOP_P    = 30,

     URX_RELOC_OPRND   = 31,   // Operand value in multi-operand ops that refers
                               //   back into compiled pattern code, and thus must
                               //   be relocated when inserting/deleting ops in code.

     URX_STO_SP        = 32,   // Store the stack ptr.  Operand is location within
                               //   matcher data (not stack data) to store it.
     URX_LD_SP         = 33,   // Load the stack pointer.  Operand is location
                               //   to load from.
     URX_BACKREF       = 34,   // Back Reference.  Parameter is the index of the
                               //   capture group variables in the state stack frame.
     URX_STO_INP_LOC   = 35,   // Store the input location.  Operand is location
                               //   within the matcher data (not stack).
     URX_JMPX          = 36,  // Conditional JMP.
                               //   First Operand:  JMP target location.
                               //   Second Operand:  Data location containing an 
                               //     input position.  If current input position ==
                               //     saved input position, FAIL rather than taking
                               //     the JMP
     URX_LA_START      = 37,   // Starting a LookAround expression.
                               //   Save InputPos and SP in static data.
                               //   Operand:  Static data offset for the save
     URX_LA_END        = 38    // Ending a Lookaround expression.
                               //   Restore InputPos and Stack to saved values.
                               //   Operand:  Static data offset for saved data.
};

// Keep this list of opcode names in sync with the above enum
//   Used for debug printing only.
#define URX_OPCODE_NAMES       \
        "               ",     \
        "URX_BACKTRACK",       \
        "END",                 \
        "ONECHAR",             \
        "STRING",              \
        "STRING_LEN",          \
        "STATE_SAVE",          \
        "NOP",                 \
        "START_CAPTURE",       \
        "END_CAPTURE",         \
        "URX_STATIC_SETREF",   \
        "SETREF",              \
        "DOTANY",              \
        "JMP",                 \
        "FAIL",                \
        "URX_BACKSLASH_A",     \
        "URX_BACKSLASH_B",     \
        "URX_BACKSLASH_G",     \
        "URX_BACKSLASH_W",     \
        "URX_BACKSLASH_X",     \
        "URX_BACKSLASH_Z",     \
        "URX_DOTANY_ALL",      \
        "URX_BACKSLASH_D",     \
        "URX_CARET",           \
        "URX_DOLLAR",          \
        "CTR_INIT",            \
        "CTR_INIT_NG",         \
        "CTR_INIT_P",          \
        "CTR_LOOP",            \
        "CTR_LOOP_NG",         \
        "CTR_LOOP_P",          \
        "RELOC_OPRND",         \
        "STO_SP",              \
        "LD_SP",               \
        "BACKREF",             \
        "STO_INP_LOC",         \
        "JMPX",                \
        "LA_START",            \
        "LA_END"

//
//  Convenience macros for assembling and disassembling a compiled operation.
//
#define URX_BUILD(type, val) (int32_t)((type << 24) | (val))
#define URX_TYPE(x)          ((x) >> 24) 
#define URX_VAL(x)           ((x) & 0xffffff)

                
//
//  Access to Unicode Sets for Perl-like composite character properties
//     The sets are accessed by the match engine for things like \w (word boundary)
//     
enum {
     URX_ISWORD_SET  = 1,
     URX_ISALNUM_SET = 2,
     URX_ISALPHA_SET = 3,
     URX_ISSPACE_SET = 4,
     URX_LAST_SET    = 5,

     URX_NEG_SET     = 0x800000          // Flag bit to reverse sense of set
                                         //   membership test.
};


//
//  Match Engine State Stack Frame Layout.
//
struct REStackFrame {
    int32_t            fInputIdx;        // Position of next character in the input string
    int32_t            fPatIdx;          // Position of next Op in the compiled pattern
    int32_t            fExtra[2];        // Extra state, for capture group start/ends
                                         //   atomic parentheses, repeat counts, etc.
                                         //   Locations assigned at pattern compile time.
};

U_NAMESPACE_END
#endif

