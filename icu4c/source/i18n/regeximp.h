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


//
//  Opcode types     In the compiled form of the regexp, these are the type, or opcodes,
//                   of the entries.
//
enum {
     URX_RESERVED_OP   = 0,
     URX_UNUSED1       = 1,
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
     URX_DOLLAR        = 24   // Also for \Z
};

// Keep this list of opcode names in sync with the above enum
//   Used for debug printing only.
#define URX_OPCODE_NAMES       \
        "URX_RESERVED_OP",     \
        "URX_UNUSED1",         \
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
        "URX_DOLLAR"

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

#endif

