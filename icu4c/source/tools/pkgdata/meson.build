fs = import('fs')
sources = fs.read('sources.txt').split()

pkgdata = executable(
  'pkgdata',
  sources,
  dependencies: toolutil_dep,
  install: true,
)
