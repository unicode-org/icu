# This is not really needed to run icuinfo and
# there are some complications. If you wish to
# debug it, uncomment and have fun.

#shared_library('plugin', 'testplug.c',
#dependencies: toolutil_dep,
#)

fs = import('fs')
sources = fs.read('sources.txt').split()

executable(
  'icuinfo',
  sources,
  dependencies: toolutil_dep,
  install: true,
)
