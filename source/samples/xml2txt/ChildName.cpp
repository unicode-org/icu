/******************************************************************************
 * Copyright (C) 2002, International Business Machines Corporation and
 * others. All Rights Reserved.
 ******************************************************************************/
#include "ChildName.h"

void ChildName ::SetName(DOMString name)
{
	Name = name;
};
void ChildName::SetNext(ChildName* next)
{
	Next = next;
};
