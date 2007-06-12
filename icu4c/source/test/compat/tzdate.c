/*
**********************************************************************
*   Copyright (C) 1998-2007, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*
* File tzdate.c
*
* Author:  Michael Ow
*
*******************************************************************************
*/

#include <stdlib.h>
#include <stdio.h>
#include <time.h>
#include <string.h>

#include "unicode/utypes.h"
#include "unicode/ustring.h"
#include "unicode/uclean.h"

#include "unicode/ucnv.h"
#include "unicode/udat.h"
#include "unicode/ucal.h"

#define SIZE 80
#define OFFSET_MONTH 1

double getSystemCurrentTime(char* systime, int year, int month, int day, int useCurrentTime);
void getICUCurrentTime(char* icutime, int year, int month, int day, int useCurrentTime, double systemtime);
void printTime(char* systime, char* icutime);

int main (int argc, char** argv) {
    char* systime;
    char* icutime;
    int year, month, day;
    int sysyear;
    int useCurrentTime;
    double systemtime;
    
    sysyear = year = month = day = 0;
    
    year = atoi(argv[1]);
    month = atoi(argv[2]);
    day = atoi(argv[3]);
    useCurrentTime = atoi(argv[4]);
    
    //format year for system time
    sysyear = year - 1900;
    
    systime = malloc(sizeof(char) * SIZE);
    icutime = malloc(sizeof(char) * SIZE);

	systemtime = getSystemCurrentTime(systime, sysyear, month, day, useCurrentTime);
	getICUCurrentTime(icutime, year, month, day, useCurrentTime, systemtime * U_MILLIS_PER_SECOND);
	
	//print out the times if failed
	if (strcmp(systime, icutime) != 0) {
		printf("Failed\n");
		printTime(systime, icutime);
	}
	
	return 0;
}

void getICUCurrentTime(char* icutime, int year, int month, int day, int useCurrentTime, double systemtime) {
	UDateFormat *fmt;
	const UChar *tz = 0;
	UChar *s = 0;
  	UDateFormatStyle style = UDAT_RELATIVE;
  	UErrorCode status = U_ZERO_ERROR;
  	int32_t len = 0;
  	
  	UCalendar *c;
  	c = NULL;
  	
  	fmt = udat_open(style, style, 0, tz, -1,NULL,0, &status);
  	
  	if (!useCurrentTime) {
	  	c = ucal_open(0, -1, uloc_getDefault(), UCAL_TRADITIONAL, &status);
	  	ucal_setDate(c, year, month - OFFSET_MONTH, day, &status);
  	}
	  	
  	len = udat_format(fmt, (UDate)systemtime, 0, len, 0, &status);
  	
  	if (status == U_BUFFER_OVERFLOW_ERROR)
  		status = U_ZERO_ERROR;
  	
    s = (UChar*) malloc(sizeof(UChar) * (len+1));
    	
    if(s == 0) 
    	goto finish;
    	
    udat_format(fmt, (UDate)systemtime, s, len + 1, 0, &status);
    
    if(U_FAILURE(status)) 
    	goto finish;

  	int i;
  	for(i = 0; i < len; i++) {
  		icutime[i] = (char)s[i];
  	}

finish:
	if (c != NULL) { 
		ucal_close(c);
	}
  	udat_close(fmt);
  	free(s);
}

double getSystemCurrentTime(char* systime, int year, int month, int day, int useCurrentTime) {
	time_t now;
	struct tm ts;
	
	time(&now);
	ts = *localtime(&now);

	if (!useCurrentTime){
		ts.tm_year = year;
		ts.tm_mon = month - OFFSET_MONTH;
		ts.tm_mday = day;
		
		now = mktime(&ts);
		
		ts = *localtime(&now);	
	}
	
	strftime(systime, sizeof(char) * 80, "%Y%m%d %I:%M %p", &ts);
	
	return (double)now;
}

void printTime(char* systime, char* icutime) {
	printf("System Time:  %s\n", systime);
	printf("ICU Time:     %s\n", icutime);		
}
