#!/usr/bin/qsh
#
# Authors:
# Ami Fixler
# Steven R. Loomis <srl@jtcsv.com>
# George Rhoten
#
# Shell script to unpax ICU and convert the files to an EBCDIC codepage.
# After extracting to EBCDIC, binary files are re-extracted without the
# EBCDIC conversion, thus restoring them to original codepage.
#
# Set the following variable to the list of binary file suffixes (extensions)

binary_suffixes='ico ICO bmp BMP jpg JPG gif GIF'

usage()
{
  echo "Enter archive filename as a parameter: $0 icu-archive.tar [strip]"
  echo "(strip is an option to remove hex '0D' carraige returns)"
}
# first make sure we at least one arg and it's a file we can read
if [ $# -eq 0 ]; then
  usage
  exit
fi
if [ ! -r $1 ]; then
  echo "$1 does not exist or cannot be read."
  usage
  exit
fi
# set up a few variables

echo ""
echo "Extracting from $1 ..."
echo ""
pax -C 819 -rcvf $1 icu/data/* icu/source/test/testdata/*

# extract files while converting them to EBCDIC
echo ""
echo "Extracting files which must be in ibm-37 ..."
echo ""
pax -C 37 -rvf $1 icu/data/* icu/source/test/testdata/*

if [ $# -gt 1 ]; then 
  if [ $2 -eq strip ]; then
    echo ""
    echo "Stripping hex 0d characters ..."
    for i in $(pax -f $1 2>/dev/null)
    do
      case $i in
        */)
         # then this entry is a directory
         ;;
        *)
          # then this entry is NOT a directory
          tr -d 
 <$i >@@@icu@tmp
          chmod +w $i
          rm $i
          mv @@@icu@tmp $i
          ;;
       esac
    done
  fi
fi

echo ""
echo "Determining binary files ..."
echo ""

for dir in `find ./icu -type d \( -name CVS -o -print \)`; do
    if [ -f $dir/CVS/Entries ]; then
        binary_files="$binary_files`cat $dir/CVS/Entries | fgrep -- -kb \
                      | cut -d / -f2 | sed -e "s%^%$dir/%" \
                      | sed -e "s%^\./%%" | tr '\n' ' '`"
    fi
done

for i in $(pax -f $1 2>/dev/null)
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
         binary_files="$binary_files $i"
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
if [ ${#binary_files} -eq 0 ]; then
  echo ""
  echo "There are no binary files to restore."
else
  echo "Restoring binary files ..."
  echo ""
  pax -C 819 -rvf $1 $binary_files
fi
echo ""
echo "$0 has completed extracting ICU from $1."
