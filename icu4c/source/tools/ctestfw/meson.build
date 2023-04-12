fs = import('fs')
sources = fs.read('sources.txt').split()

ctestfw = library(
  'ctestfw',
  sources,
  c_args: '-DT_CTEST_IMPLEMENTATION',
  cpp_args: '-DT_CTEST_IMPLEMENTATION',
  dependencies: toolutil_dep,
)

ctestfw_dep = declare_dependency(
  link_with: ctestfw,
  dependencies: toolutil_dep,
)
