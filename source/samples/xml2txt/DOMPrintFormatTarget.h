/******************************************************************************
 * Copyright (C) 2002, International Business Machines Corporation and
 * others. All Rights Reserved.
 ******************************************************************************/
#ifndef DOMPRINTFORMATTARGET_H_
#define DOMPRINTFORMATTARGET_H_
#include "ChildName.h"
#include <fstream.h>
class DOMPrintFormatTarget : public XMLFormatTarget
{
private: 
	char* fileName;
public:
    DOMPrintFormatTarget();
	DOMPrintFormatTarget(char* fileName);
    ~DOMPrintFormatTarget();
    // -----------------------------------------------------------------------
    //  Implementations of the format target interface
    // -----------------------------------------------------------------------

    void writeChars(const   XMLByte* const  toWrite,
                    const   unsigned int    count,
                            XMLFormatter * const formatter);

private:
    // -----------------------------------------------------------------------
    //  Unimplemented methods.
    // -----------------------------------------------------------------------
    DOMPrintFormatTarget(const DOMPrintFormatTarget& other);
    void operator=(const DOMPrintFormatTarget& rhs);
};

#endif
