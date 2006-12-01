#!/usr/bin/qsh
#   Copyright (C) 2000-2006, International Business Machines
#   Corporation and others.  All Rights Reserved.
#
# Authors:
# Ami Fixler
# Barry Novinger
# Steven R. Loomis
# George Rhoten
#
# Shell script to unpax ICU and convert the files to an EBCDIC codepage.
# After extracting to EBCDIC, binary files are re-extracted without the
# EBCDIC conversion, thus restoring them to original codepage.
#
# Set the following variable to the list of binary file suffixes (extensions)

#binary_suffixes='ico ICO bmp BMP jpg JPG gif GIF brk BRK'
#ICU specific binary files
binary_suffixes='brk BRK bin BIN res RES cnv CNV dat DAT icu ICU spp SPP xml XML'
data_files='icu/source/data/brkitr/* icu/source/data/locales/* icu/source/data/coll/* icu/source/data/rbnf/* icu/source/data/mappings/* icu/source/data/misc/* icu/source/data/translit/* icu/source/data/unidata/* icu/source/test/testdata/*'

usage()
{
  echo "Enter archive filename as a parameter: $0 icu-archive.tar"
}
# first make sure we at least one arg and it's a file we can read
if [ $# -eq 0 ]; then
  usage
  exit
fi
tar_file=$1
if [ ! -r $tar_file ]; then
  echo "$tar_file does not exist or cannot be read."
  usage
  exit
fi
# set up a few variables

echo ""
echo "Extracting from $tar_file ..."
echo ""

# determine which directories in the data_files list
# are included in the provided archive
for data_dir in $data_files
do
    if (pax -f $tar_file $data_dir >/dev/null 2>&1)
    then
        ebcdic_data="$ebcdic_data `echo $data_dir`";
    fi
done

# extract everything as iso-8859-1 except these directories
pax -C 819 -rcvf $tar_file $ebcdic_data

# extract files while converting them to EBCDIC
echo ""
echo "Extracting files which must be in ibm-37 ..."
echo ""
pax -C 37 -rvf $tar_file $ebcdic_data

echo ""
echo "Determining binary files ..."
echo ""

for file in `find ./icu \( -name \*.txt -print \)`; do
    bom8=`head -n 1 $file|\
          od -t x1|\
          head -n 1|\
          sed 's/  */ /g'|\
          cut -f2-4 -d ' '|\
          tr 'A-Z' 'a-z'`;
    #Find a converted UTF-8 BOM
    if [ "$bom8" = "057 08b 0ab" -o "$bom8" = "57 8b ab" ]
    then
        if [ `echo $binary_files1 | wc -w` -lt 250 ]
        then
            binary_files1="$binary_files1 `echo $file | cut -d / -f2-`";
        else
            binary_files2="$binary_files2 `echo $file | cut -d / -f2-`";
        fi
    fi
done

for i in $(pax -f $tar_file 2>/dev/null)
do
  case $i in
    */)
#    then this entry is a directory
     ;;
    *.*)
#    then this entry has a dot in the filename
     for j in $binary_suffixes
     do
       suf=${i#*.*}
       if [ "$suf" = "$j" ]
       then
         binary_files2="$binary_files2 $i"
         break
       fi
     done
     ;;
    *)
#    then this entry does not have a dot in it
     ;;
  esac
done

# now see if a re-extract of binary files is necessary
if [ ${#binary_files1} -eq 0 -a ${#binary_files2} -eq 0 ]; then
  echo ""
  echo "There are no binary files to restore."
else
  echo "Restoring binary files ..."
  echo ""
  rm $binary_files1
  rm $binary_files2
  pax -C 819 -rvf $tar_file $binary_files1
  pax -C 819 -rvf $tar_file $binary_files2
fi

echo ""
echo "Generating qsh compatible configure ..."
echo ""

sed -f icu/as_is/os400/convertConfigure.sed icu/source/configure > icu/source/configureTemp
del -f icu/source/configure
mv icu/source/configureTemp icu/source/configure
chmod 755 icu/source/configure

echo ""
echo "$0 has completed extracting ICU from $tar_file."
