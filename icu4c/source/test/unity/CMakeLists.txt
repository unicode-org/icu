cmake_minimum_required(VERSION 3.22)
project(icu4c_unity_compile LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_EXTENSIONS OFF)
set(CMAKE_UNITY_BUILD ON)
set(CMAKE_UNITY_BUILD_BATCH_SIZE 50 CACHE STRING "Number of sources per unity batch")

get_filename_component(ICU_SOURCE_ROOT "${CMAKE_CURRENT_LIST_DIR}/../.." ABSOLUTE)

file(STRINGS "${ICU_SOURCE_ROOT}/common/sources.txt" COMMON_SOURCES)
file(STRINGS "${ICU_SOURCE_ROOT}/i18n/sources.txt" I18N_SOURCES)

foreach(source IN LISTS COMMON_SOURCES)
  list(APPEND COMMON_PATHS "${ICU_SOURCE_ROOT}/common/${source}")
endforeach()
foreach(source IN LISTS I18N_SOURCES)
  list(APPEND I18N_PATHS "${ICU_SOURCE_ROOT}/i18n/${source}")
endforeach()

add_library(icu_unity_common OBJECT ${COMMON_PATHS})
target_include_directories(icu_unity_common PRIVATE "${ICU_SOURCE_ROOT}/common")
target_compile_definitions(icu_unity_common PRIVATE
  U_ALL_IMPLEMENTATION U_COMMON_IMPLEMENTATION U_ATTRIBUTE_DEPRECATED=)

add_library(icu_unity_i18n OBJECT ${I18N_PATHS})
target_include_directories(icu_unity_i18n PRIVATE
  "${ICU_SOURCE_ROOT}/i18n" "${ICU_SOURCE_ROOT}/common")
target_compile_definitions(icu_unity_i18n PRIVATE
  U_ALL_IMPLEMENTATION U_I18N_IMPLEMENTATION U_ATTRIBUTE_DEPRECATED=
  UNISTR_FROM_STRING_EXPLICIT= UNISTR_FROM_CHAR_EXPLICIT=)
