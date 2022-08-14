add_library(toolutil)
target_link_libraries(toolutil PRIVATE icu_lib_defaults icuuc icui18n)
target_include_directories(toolutil PUBLIC ${CMAKE_CURRENT_SOURCE_DIR})
target_compile_definitions(toolutil PRIVATE U_TOOLUTIL_IMPLEMENTATION)
target_sources(toolutil PRIVATE
    collationinfo.cpp
    dbgutil.cpp
    denseranges.cpp
    filestrm.cpp
    filetools.cpp
    flagparser.cpp
    package.cpp
    pkg_genc.cpp
    pkg_gencmn.cpp
    pkg_icu.cpp
    pkgitems.cpp
    ppucd.cpp
    swapimpl.cpp
    toolutil.cpp
    ucbuf.cpp
    ucln_tu.cpp
    ucm.cpp
    ucmstate.cpp
    udbgutil.cpp
    unewdata.cpp
    uoptions.cpp
    uparse.cpp
    writesrc.cpp
    xmlparser.cpp)
