@echo off
chcp 65001 > nul
set GRADLE_OPTS=-Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8
.\gradlew.bat run %*
