/*
*******************************************************************************
*
*   Copyright (C) 2000-2003, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  genmbcs.h
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2000jul10
*   created by: Markus W. Scherer
*/

#ifndef __GENMBCS_H__
#define __GENMBCS_H__

#include "makeconv.h"

enum {
    MBCS_STAGE_2_BLOCK_SIZE=0x40, /* 64; 64=1<<6 for 6 bits in stage 2 */
    MBCS_STAGE_2_BLOCK_SIZE_SHIFT=6, /* log2(MBCS_STAGE_2_BLOCK_SIZE) */
    MBCS_STAGE_1_SIZE=0x440, /* 0x110000>>10, or 17*64 for one entry per 1k code points */
    MBCS_STAGE_2_SIZE=0xfbc0, /* 0x10000-MBCS_STAGE_1_SIZE */
    MBCS_MAX_STAGE_2_TOP=MBCS_STAGE_2_SIZE,
    MBCS_STAGE_2_MAX_BLOCKS=MBCS_STAGE_2_SIZE>>MBCS_STAGE_2_BLOCK_SIZE_SHIFT,

    MBCS_STAGE_2_ALL_UNASSIGNED_INDEX=0, /* stage 1 entry for the all-unassigned stage 2 block */
    MBCS_STAGE_2_FIRST_ASSIGNED=MBCS_STAGE_2_BLOCK_SIZE, /* start of the first stage 2 block after the all-unassigned one */

    MBCS_STAGE_3_BLOCK_SIZE=16, /* 16; 16=1<<4 for 4 bits in stage 3 */
    MBCS_STAGE_3_FIRST_ASSIGNED=MBCS_STAGE_3_BLOCK_SIZE, /* start of the first stage 3 block after the all-unassigned one */

    MBCS_MAX_FALLBACK_COUNT=8192
};

U_CFUNC NewConverter *
MBCSOpen(UCMFile *ucm);

U_CFUNC NewConverter *
CnvExtOpen(UCMFile *ucm);

#endif
