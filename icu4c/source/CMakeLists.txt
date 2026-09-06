cmake_minimum_required(VERSION 3.16)

project(icu4c)

set(CMAKE_ARCHIVE_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/lib)
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/bin)
set(CMAKE_RUNTIME_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/bin)

add_library(icu_defaults INTERFACE)
target_compile_definitions(icu_defaults INTERFACE U_ATTRIBUTE_DEPRECATED=)

if(WIN32)
    target_compile_definitions(icu_defaults INTERFACE
        $<$<NOT:$<BOOL:${BUILD_SHARED_LIBS}>>:U_STATIC_IMPLEMENTATION>
        WIN64
        WIN32
        WINVER=0x0601
        _WIN32_WINNT=0x0601
        _CRT_SECURE_NO_DEPRECATE
        _MBCS
        _HAS_EXCEPTIONS=0
        $<$<CONFIG:Debug>:_DEBUG>)
else()
    target_compile_definitions(icu_defaults INTERFACE
        _REENTRANT U_HAVE_ELF_H=1 U_HAVE_STRTOD_L=1 U_HAVE_XLOCALE_H=0)
endif()

if(MSVC)
    target_compile_options(icu_defaults INTERFACE /utf-8)
endif()

add_library(icu_lib_defaults INTERFACE)
target_link_libraries(icu_lib_defaults INTERFACE icu_defaults)

if(WIN32)
    target_compile_definitions(icu_lib_defaults INTERFACE _HAS_EXCEPTIONS=0)
endif()

macro(icu_add_sources target_name)
    file(STRINGS sources.txt sources)
    target_sources("${target_name}" PRIVATE "${sources}")
endmacro()

add_subdirectory(common)
add_subdirectory(i18n)
add_subdirectory(io)
add_subdirectory(tools)
add_subdirectory(stubdata)

include(CTest)

if(BUILD_TESTING)
    add_test(NAME icuinfo COMMAND icuinfo WORKING_DIRECTORY ${CMAKE_RUNTIME_OUTPUT_DIRECTORY})
    add_subdirectory(test)
endif()
