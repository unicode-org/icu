add_library(icudt)
icu_add_sources(icudt)
target_link_libraries(icudt PRIVATE icu_lib_defaults icuuc)
target_compile_definitions(icudt PRIVATE U_COMMON_IMPLEMENTATION)

add_custom_command(
  TARGET icudt POST_BUILD
  COMMAND ${CMAKE_COMMAND} -E echo "File with stubdata build time, used as a dependency to trigger fresh data build, since stubdata dll will overwrite the real one." > "${CMAKE_CURRENT_LIST_DIR}/stubdatabuilt.txt"
	WORKING_DIRECTORY ${icu4c_SOURCE_DIR}/extra/uconv
	VERBATIM)
