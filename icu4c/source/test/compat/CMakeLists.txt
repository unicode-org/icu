add_executable(compat tzdate.c)
target_link_libraries(compat PRIVATE icu_defaults icuuc icui18n)