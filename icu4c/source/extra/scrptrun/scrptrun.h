/*
 * %W% %E%
 *
 * (C) Copyright IBM Corp. 2001 - All Rights Reserved
 *
 */

#ifndef __SCRPTRUN_H
#define __SCRPTRUN_H

#include "layout/LETypes.h"

struct ScriptRecord
{
	LEUnicode32 startChar;
	LEUnicode32 endChar;
	le_int32 scriptCode;
};

class ScriptRun
{
public:
	ScriptRun();

	ScriptRun(const LEUnicode chars[], le_int32 length);

	ScriptRun(const LEUnicode chars[], le_int32 start, le_int32 length);

	void reset();

	void reset(le_int32 start, le_int32 count);

	void reset(const LEUnicode chars[], le_int32 start, le_int32 length);

	le_int32 getScriptStart();

	le_int32 getScriptEnd();

	le_int32 getScriptCode();

	le_bool next();

private:

	static le_bool sameScript(le_int32 scriptOne, le_int32 scriptTwo);

	le_int32 charStart;
	le_int32 charLimit;
	const LEUnicode *charArray;

	le_int32 scriptStart;
	le_int32 scriptEnd;
	le_int32 scriptCode;
};

inline ScriptRun::ScriptRun()
{
	reset(NULL, 0, 0);
}

inline ScriptRun::ScriptRun(const LEUnicode chars[], le_int32 length)
{
	reset(chars, 0, length);
}

inline ScriptRun::ScriptRun(const LEUnicode chars[], le_int32 start, le_int32 length)
{
	reset(chars, start, length);
}

inline le_int32 ScriptRun::getScriptStart()
{
	return scriptStart;
}

inline le_int32 ScriptRun::getScriptEnd()
{
	return scriptEnd;
}

inline le_int32 ScriptRun::getScriptCode()
{
	return scriptCode;
}

inline void ScriptRun::reset()
{
	scriptStart = charStart;
	scriptEnd   = charStart;
	scriptCode  = -1;
}

inline void ScriptRun::reset(le_int32 start, le_int32 length)
{
	charStart = start;
	charLimit = start + length;

	reset();
}

inline void ScriptRun::reset(const LEUnicode chars[], le_int32 start, le_int32 length)
{
	charArray = chars;

	reset(start, length);
}


#endif
