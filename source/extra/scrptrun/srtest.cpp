/*
 * %W% %E%
 *
 * (C) Copyright IBM Corp. 2001 - All Rights Reserved
 *
 */

#include "layout/LETypes.h"
#include "unicode/uscript.h"

#include "scrptrun.h"

#include <stdio.h>

LEUnicode testChars[] = {
            0x0020, 0x0946, 0x0939, 0x093F, 0x0928, 0x094D, 0x0926, 0x0940, 0x0020,
            0x0627, 0x0644, 0x0639, 0x0631, 0x0628, 0x064A, 0x0629, 0x0020,
            0x0420, 0x0443, 0x0441, 0x0441, 0x043A, 0x0438, 0x0439, 0x0020,
            'E', 'n', 'g', 'l', 'i', 's', 'h',  0x0020,
            0x6F22, 0x5B75, 0x3068, 0x3072, 0x3089, 0x304C, 0x306A, 0x3068, 
            0x30AB, 0x30BF, 0x30AB, 0x30CA,
            0xD801, 0xDC00, 0xD801, 0xDC01, 0xD801, 0xDC02, 0xD801, 0xDC03
};

le_int32 testLength = sizeof testChars / sizeof testChars[0];

void main()
{
    ScriptRun scriptRun(testChars, 0, testLength);

    while (scriptRun.next()) {
        le_int32 start = scriptRun.getScriptStart();
        le_int32 end   = scriptRun.getScriptEnd();
        le_int32 code  = scriptRun.getScriptCode();

		printf("Script '%s' from %d to %d.\n", uscript_getName((UScriptCode) code), start, end);
	}
}