# Copyright (C) 2016 International Business Machines Corporation
# and others. All rights reserved.
#
# Run this script from $ICU_ROOT/src/source/

CXX=clang++

for file in `ls common/*.h`; do
    echo $file
    echo '#include "'$file'"' > ht_temp.cpp ;
    echo 'void noop() {}' >> ht_temp.cpp ;
    $CXX -c -I common -O0 ht_temp.cpp ;
done ;

for file in `ls i18n/*.h`; do
    echo $file
    echo '#include "'$file'"' > ht_temp.cpp ;
    echo 'void noop() {}' >> ht_temp.cpp ;
    $CXX -c -I common -I i18n -O0 ht_temp.cpp ;
done ;

for file in `ls io/*.h`; do
    echo $file
    echo '#include "'$file'"' > ht_temp.cpp ;
    echo 'void noop() {}' >> ht_temp.cpp ;
    $CXX -c -I common -I i18n -I io -O0 ht_temp.cpp ;
done ;

rm ht_temp.cpp ht_temp.o
