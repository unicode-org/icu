Readme file for ICU time zone data (source/tools/gentz)

The time zone data in ICU is taken from the UNIX data files at
ftp://elsie.nci.nih.gov/pub/tzdata<year>.

Two tools are used to process the data into a format suitable for ICU:

   tz.pl    directory of raw data files -> tz.txt
   gentz    tz.txt -> tz.dat (memory mappable binary file)

After gentz is run, standard ICU data tools are used to incorporate
tz.dat into the icudata module.

In order to incorporate the raw data from that source into ICU, take
the following steps.

1. Download the archive of current zone data.  This should be a file
   named something like tzdata1999j.tar.gz.  Use the URL listed above.

2. Unpack the archive into a directory, retaining the name of the
   archive.  For example, unpack tzdata1999j.tar.gz into tzdata1999j/.
   Place this directory anywhere; one option is to place it within
   source/tools/gentz.

3. Run the perl script tz.pl, passing it the directory location as a
   command-line argument.  On Windows system use the batch file
   tz.bat.  The output of this step is the intermediate text file
   source/tools/gentz/tz.txt.

4. Run source/tools/makedata on Windows.  On UNIX systems the
   equivalent build steps are performed by 'make' and 'make install'.

The tz.txt file is typically checked into CVS, whereas the raw data
files are not, since they are readily available from the URL listed
above.

Alan Liu 1999
