/******************************************************************************
 * Copyright (C) 2002, International Business Machines Corporation and
 * others. All Rights Reserved.
 ******************************************************************************/
#include "DOMPrintFormatTarget.h"

DOMPrintFormatTarget::DOMPrintFormatTarget() {

};

DOMPrintFormatTarget::DOMPrintFormatTarget(char* fileName) {
	this->fileName = fileName;
}

DOMPrintFormatTarget::~DOMPrintFormatTarget() {};

void DOMPrintFormatTarget :: writeChars(const   XMLByte* const  toWrite,
                   const   unsigned int    count,
                   XMLFormatter * const formatter)
{
	ofstream ofile( fileName, ios::app);
	ofile.write((char *) toWrite, (int) count);
};
