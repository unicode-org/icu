add_library(icuuc)
icu_add_sources(icuuc)
target_link_libraries(icuuc PRIVATE icu_defaults icu_lib_defaults icudt)
target_include_directories(icuuc PUBLIC ${CMAKE_CURRENT_SOURCE_DIR})
target_compile_definitions(icuuc PRIVATE U_COMMON_IMPLEMENTATION)

if(WIN32)
    target_compile_definitions(icuuc PRIVATE $<$<CONFIG:Debug>:RBBI_DEBUG> U_PLATFORM_USES_ONLY_WIN32_API=1)
endif()
