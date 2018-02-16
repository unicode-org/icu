# Copyright (C) 2016 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html
#-------------------------
# Script: icu\packaging\distrelease.ps1
# Author: Steven R. Loomis
# Date: 2017-04-14
#-------------------------
#
# This builds a zipfile containing the *64 bit* Windows binaries.
# (Note: The zipfile does not include the UWP binaries.)
#
# Usage: (after building ICU using MSVC) 
#  (bring up Powershell ISE)
#    cd C:\icu\icu4c\
#    Set-ExecutionPolicy -Scope Process Unrestricted
#    .\packaging\distrelease.ps1
#
# Will emit: c:\icu4c\icu\source\dist\icu-windows.zip
#
#
# You will get warnings from the execution policy and the script itself.
#  see https://docs.microsoft.com/powershell/module/microsoft.powershell.core/about/about_execution_policies?view=powershell-5.1&viewFallbackFrom=powershell-Microsoft.PowerShell.Core 
#    for more about execution policies.


$icuDir = Split-Path -Path $MyInvocation.MyCommand.Definition -Parent
$icuDir = Resolve-Path -Path '$icuDir\..'

echo  $icuDir

# ok, create some work areas
New-Item -Path "$icuDir\source\dist" -ErrorAction SilentlyContinue -ItemType "directory"
$source = "$icuDir\source\dist\icu"
Get-ChildItem -Path $source -ErrorAction SilentlyContinue | Remove-Item -Recurse
New-Item -Path $source -ItemType "directory" -ErrorAction SilentlyContinue

# copy required stuff
Copy-Item -Path "$icuDir\lib64" -Destination $source -Recurse
Copy-Item -Path "$icuDir\include" -Destination $source -Recurse
Copy-Item -Path "$icuDir\bin64" -Destination $source -Recurse
Copy-Item -Path "$icuDir\APIChangeReport.html" -Destination $source -Recurse
Copy-Item -Path "$icuDir\icu4c.css" -Destination $source -Recurse
Copy-Item -Path "$icuDir\LICENSE" -Destination $source -Recurse
Copy-Item -Path "$icuDir\readme.html" -Destination $source -Recurse


$destination = "$icuDir\source\dist\icu-windows.zip"
Remove-Item -Path $destination -ErrorAction Continue
Add-Type -assembly "system.io.compression.filesystem"
Echo $source
Echo $destination
[io.compression.zipfile]::CreateFromDirectory($source, $destination)

echo $destination