// 
//   Copyright (C) 2002 International Business Machines Corporation 
//   and others. All rights reserved.  
//
//   file:  regeximp.h
//
//           ICU Regular Expressions, declarations of internal implementation types
//           and constants that are common between the pattern compiler and the 
//           runtime execution engine.
//

#ifndef _REGEXIMP_H
#define _REGEXIMP_H


//
//  Opcode types     In the compiled form of the regex, these are the type, or opcodes,
//                   of the entries.
//
static const uint32_t     URX_UNUSED1       = 1;
static const uint32_t     URX_END           = 2;
static const uint32_t     URX_ONECHAR       = 3;
static const uint32_t     URX_STRING        = 4;    // Value field is index of string start
static const uint32_t     URX_STRING_LEN    = 5;    // Value field is string length (code units)
static const uint32_t     URX_STATE_SAVE    = 6;    // Value field is pattern position to push
static const uint32_t     URX_NOP           = 7;
static const uint32_t     URX_START_CAPTURE = 8;    // Value field is capture group number.
static const uint32_t     URX_END_CAPTURE   = 9;    // Value field is capture group number
static const uint32_t     URX_UNUSED10      = 10;   // Value field is index in pattern to
                                                    //   loop back to.
static const uint32_t     URX_SETREF        = 11;   // Value field is index of set in array of sets.
static const uint32_t     URX_DOTANY        = 12; 
static const uint32_t     URX_JMP           = 13;   // Value field is destination position in
                                                    //   the pattern.
static const uint32_t     URX_FAIL          = 14;   // Stop match operation;  No match.

//
//  Convenience macros for assembling and disassembling a compiled operation.
//
#define URX_BUILD(type, val) (int32_t)((type << 24) | (val))
#define URX_TYPE(x)          ((x) >> 24) 
#define URX_VAL(x)           ((x) & 0xffffff)

                
#endif

