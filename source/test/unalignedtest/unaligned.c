/*
    This program is a wrapper to assist in debugging analigned traps on the Alpha
    architectures.

    COPYRIGHT AND PERMISSION NOTICE

    Copyright (c) 2002 Sean Hunter

    Permission is hereby granted, free of charge, to any person obtaining a
    copy of this software and associated documentation files (the
    "Software"), to deal in the Software without restriction, including
    without limitation the rights to use, copy, modify, merge, publish,
    distribute, and/or sell copies of the Software, and to permit persons
    to whom the Software is furnished to do so, provided that the above
    copyright notice(s) and this permission notice appear in all copies of
    the Software and that both the above copyright notice(s) and this
    permission notice appear in supporting documentation.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
    OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
    MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT
    OF THIRD PARTY RIGHTS. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
    HOLDERS INCLUDED IN THIS NOTICE BE LIABLE FOR ANY CLAIM, OR ANY SPECIAL
    INDIRECT OR CONSEQUENTIAL DAMAGES, OR ANY DAMAGES WHATSOEVER RESULTING
    FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
    NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
    WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.

    Except as contained in this notice, the name of a copyright holder
    shall not be used in advertising or otherwise to promote the sale, use
    or other dealings in this Software without prior written authorization
    of the copyright holder.

    --------------------------------------------------------------------------------
    All trademarks and registered trademarks mentioned herein are the property
    of their respective owners.  

*/
#include <errno.h>
#include <stdio.h>

#include <asm/sysinfo.h>
#include <asm/unistd.h>

#define TMP_PATH_MAX 1024


static int 
setsysinfo(unsigned long op, void *buffer, unsigned long size,
		      int *start, void *arg, unsigned long flag)
{
	syscall(__NR_osf_setsysinfo, op, buffer, size, start, arg, flag);
}


void 
trap_unaligned(void)
{
	unsigned int buf[2];
	buf[0] = SSIN_UACPROC;
	buf[1] = UAC_SIGBUS | UAC_NOPRINT;
	setsysinfo(SSI_NVPAIRS, buf, 1, 0, 0, 0);
}


static void 
usage(void)
{
        fprintf(stderr,
		"usage: unaligned [-b] <command-path> [command-args...]\n\n"
		"  This program is designed to assist debugging of\n"
		"  unaligned traps by running the program in gdb\n"
		"  and causing it to get SIGBUS when it encounters\n"
		"  an unaligned trap.\n\n"
		"  It is free software written by Sean Hunter <sean@uncarved.co.uk>\n"
		"  based on code by Richard Henderson and Andrew Morgan.\n\n"
	);

	exit(1);
}


int 
main(int argc, char **argv)
{
	const char my_debugger[] = "/usr/bin/gdb";

	char *temp_str;
	char *curr;
	int size = 0;
	int curr_arg;
	int isBatchMode = 0;

	/* check that we have at least 1 argument */
	if (argc < 2) {
		usage();
	}
	if( strcmp("-b" , argv[1]) == 0 ){
	    isBatchMode = 1;
	    curr_arg = 2;
        }else{
	    curr_arg = 1;
	}        

	trap_unaligned();

	if (argc > 2) {
		/* We're going to use bash process redirection to create a "file" for gdb to read
		 * containing the arguments we need */
		size = 2048;
		for(; curr_arg < argc; curr_arg++) {
			size += strlen(argv[curr_arg]);
		}
		temp_str = (char *) malloc(sizeof(char) * size);
		if (!temp_str) {
		    fprintf(stderr, "Unable to malloc memory for string use: %s\n", strerror(errno));
		    exit(255);
		}
		if(isBatchMode==1){
			sprintf(temp_str, "%s -batch %s -x <( echo file %s; echo set args", my_debugger, argv[2], argv[2]);
	        }else{
			sprintf(temp_str, "%s %s -x <( echo file %s; echo set args", my_debugger, argv[1], argv[1]);
		}
		curr = temp_str + strlen(temp_str);
		for(curr_arg = 2; curr_arg < argc; curr_arg++) {
	    		sprintf(curr, " %s", argv[curr_arg]);
			curr = temp_str + strlen(temp_str);
		}
#ifndef NOAUTORUN
		curr = temp_str + strlen(temp_str);
		sprintf(curr, "; echo run");
#endif 
		curr = temp_str + strlen(temp_str);
		sprintf(curr, ")");

		execlp("/bin/bash", "/bin/bash", "-c", temp_str, NULL);

	}
	else {
		execlp(my_debugger, my_debugger, argv[1], NULL);
	}	

	/* if we fall through to here, our exec failed -- announce the fact */
	fprintf(stderr, "Unable to execute command: %s\n", strerror(errno));

	usage();

}

/* use gcc unaligned.c -o unaliged to compile.  Add -DNOAUTORUN if you
don't want gdb to automatically run the program */

