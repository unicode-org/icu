/*
 * %W% %E%
 *
 * (C) Copyright IBM Corp. 2001 - All Rights Reserved
 *
 */

#include "layout/LETypes.h"
#include "layout/LEScripts.h"
#include "unicode/uscript.h"

#include "scrptrun.h"

le_bool ScriptRun::sameScript(le_int32 scriptOne, le_int32 scriptTwo)
{
	return scriptOne <= qaaiScriptCode || scriptTwo <= qaaiScriptCode || scriptOne == scriptTwo;
}

le_bool ScriptRun::next()
{
    UErrorCode error = U_ZERO_ERROR;

    if (scriptEnd >= charLimit) {
        return false;
    }
    
    scriptCode = zyyyScriptCode;

    for (scriptStart = scriptEnd; scriptEnd < charLimit; scriptEnd += 1) {
        LEUnicode16 high = charArray[scriptEnd];
		LEUnicode32 ch = high;

		if (scriptEnd < charLimit - 1 && high >= 0xD800 && high <= 0xDBFF)
		{
			LEUnicode16 low = charArray[scriptEnd + 1];

			if (low >= 0xDC00 && low <= 0xDFFF) {
				ch = (high - 0xD800) * 0x0400 + low - 0xDC00 + 0x10000;
				scriptEnd += 1;
			}
		}

        le_int32 sc = uscript_getScript(ch, &error);

        if (sameScript(scriptCode, sc)) {
            if (scriptCode <= qaaiScriptCode && sc > qaaiScriptCode) {
                scriptCode = sc;
            }
        } else {
            // if the run broke on a surrogate pair,
            // end it before the high surrogate
            if (ch >= 0x10000) {
                scriptEnd -= 1;
            }

            break;
        }
    }

    return true;
}

