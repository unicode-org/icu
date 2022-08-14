add_executable(genrb)
target_link_libraries(genrb PRIVATE icu_defaults icuuc toolutil icui18n)
target_sources(genrb PRIVATE
    errmsg.c
    filterrb.cpp
    genrb.cpp
    parse.cpp
    prscmnts.cpp
    rbutil.c
    read.c
    reslist.cpp
    rle.c
    ustr.c
    wrtjava.cpp
    wrtxml.cpp)

add_executable(derb derb.cpp)
target_link_libraries(derb PRIVATE icu_defaults icuuc toolutil icuio icui18n)