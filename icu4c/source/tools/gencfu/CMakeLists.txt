add_executable(gencfu)
icu_add_sources(gencfu)
target_link_libraries(gencfu PRIVATE icu_defaults icuuc toolutil icui18n)