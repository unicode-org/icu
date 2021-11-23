cmake_minimum_required(VERSION 3.0.0)
project(uci VERSION 0.1.0)

include(CTest)
enable_testing()

set(CMAKE_GENERATOR_PLATFORM Win32)

if(MSVC)
    add_compile_options(/w44273)
    add_compile_options("$<$<C_COMPILER_ID:MSVC>:/utf-8>")
    add_compile_options("$<$<CXX_COMPILER_ID:MSVC>:/utf-8>")
    add_definitions(-D_CRT_SECURE_NO_DEPRECATE)
    add_definitions(-DCMAKE_WINDOWS_EXPORT_ALL_SYMBOLS)
    add_definitions(-D_WINDLL)
    add_definitions(-D_MBCS)
    add_definitions(-D_HAS_EXCEPTIONS=0)
    add_definitions(-D_DEBUG)
    add_definitions(/W3)
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} /Zc:wchar_t /Zc:inline /fp:precise")
    find_program(CMAKE_MAKE_PROGRAM
        NAMES mingw32-make
              mingw32-make.exe
        DOC "Find a suitable make program for building under Windows/MinGW"
        HINTS c:/MinGW-32/bin)
endif()

add_compile_definitions(TRUE=1)
add_compile_definitions(FALSE=1)

set(CMAKE_POSITION_INDEPENDENT_CODE ON)

set(UCI_ROOT ${CMAKE_CURRENT_SOURCE_DIR})
set(CMAKE_RUNTIME_OUTPUT_DIRECTORY ${CMAKE_SOURCE_DIR}/bin)
set(CMAKE_ARCHIVE_OUTPUT_DIRECTORY ${CMAKE_SOURCE_DIR}/lib)
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${CMAKE_SOURCE_DIR}/lib)

include_directories(../include common i18n)

add_subdirectory(common)
add_subdirectory(i18n)
add_subdirectory(io)

set(CPACK_PROJECT_NAME ${PROJECT_NAME})
set(CPACK_PROJECT_VERSION ${PROJECT_VERSION})
include(CPack)
