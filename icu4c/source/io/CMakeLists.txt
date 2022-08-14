add_library(icuio)
target_link_libraries(icuio PRIVATE icu_lib_defaults icuuc icui18n)
target_compile_definitions(icuio PRIVATE U_IO_IMPLEMENTATION)
target_include_directories(icuio PUBLIC ${CMAKE_CURRENT_SOURCE_DIR})
target_sources(icuio PRIVATE
    locbund.cpp
    sprintf.cpp
    sscanf.cpp
    ucln_io.cpp
    ufile.cpp
    ufmt_cmn.cpp
    uprintf.cpp
    uprntf_p.cpp
    uscanf.cpp
    uscanf_p.cpp
    ustdio.cpp
    ustream.cpp)
