/******************************************************************************
 * Copyright (C) 2002, International Business Machines Corporation and
 * others. All Rights Reserved.
 ******************************************************************************/
#ifndef CHILDNAME_H_
#define CHILDNAME_H_
#include <util/PlatformUtils.hpp>
#include <util/XMLString.hpp>
#include <util/XMLUniDefs.hpp>
#include <framework/XMLFormatter.hpp>
#include <util/TranscodingException.hpp>
#include <dom/DOM_DOMException.hpp>
#include <parsers/DOMParser.hpp>
#include <dom/DOM.hpp>
#include "DOMTreeErrorReporter.hpp"

class ChildName
{
	public:
		DOMString Name;
		ChildName* Next;

		ChildName(){};

		void SetName(DOMString name);
		void SetNext(ChildName* next);
};

#endif
