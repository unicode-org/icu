/******************************************************************************
 * Copyright (C) 2002, International Business Machines Corporation and
 * others. All Rights Reserved.
 ******************************************************************************/
// ---------------------------------------------------------------------------
//  Includes
// ---------------------------------------------------------------------------
#include <sax/SAXParseException.hpp>
#include "DOMTreeErrorReporter.hpp"
#include <iostream.h>
#include <stdlib.h>
#include <string.h>
#include <dom/DOMString.hpp>

extern ostream& operator<<(ostream& target, const DOMString& s);

void DOMTreeErrorReporter::warning(const SAXParseException&)
{
}

void DOMTreeErrorReporter::error(const SAXParseException& toCatch)
{
    fSawErrors = true;
    cerr << "Error at file \"" << DOMString(toCatch.getSystemId())
		 << "\", line " << toCatch.getLineNumber()
		 << ", column " << toCatch.getColumnNumber()
         << "\n   Message: " << DOMString(toCatch.getMessage()) << endl;
}

void DOMTreeErrorReporter::fatalError(const SAXParseException& toCatch)
{
    fSawErrors = true;
    cerr << "Fatal Error at file \"" << DOMString(toCatch.getSystemId())
		 << "\", line " << toCatch.getLineNumber()
		 << ", column " << toCatch.getColumnNumber()
         << "\n   Message: " << DOMString(toCatch.getMessage()) << endl;
}

void DOMTreeErrorReporter::resetErrors()
{
}
