add_executable(gencmn)
icu_add_sources(gencmn)
target_link_libraries(gencmn PRIVATE icu_defaults icuuc toolutil)