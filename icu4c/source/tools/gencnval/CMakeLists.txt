add_executable(gencnval)
icu_add_sources(gencnval)
target_link_libraries(gencnval PRIVATE icu_defaults icuuc toolutil)