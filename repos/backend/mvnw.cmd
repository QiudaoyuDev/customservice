@echo off
setlocal EnableExtensions EnableDelayedExpansion
set "WRAPPER_DIR=%~dp0.mvn\wrapper"
set "PROPS=%WRAPPER_DIR%\maven-wrapper.properties"
set "DIST_DIR=%WRAPPER_DIR%\dists\apache-maven-3.9.11"
set "MVN_CMD=%DIST_DIR%\apache-maven-3.9.11\bin\mvn.cmd"

if not exist "!MVN_CMD!" (
  if not exist "!DIST_DIR!" mkdir "!DIST_DIR!"
  set "ARCHIVE=%DIST_DIR%\apache-maven-3.9.11-bin.zip"
  if not exist "!ARCHIVE!" (
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -UseBasicParsing -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.11/apache-maven-3.9.11-bin.zip' -OutFile '!ARCHIVE!'"
    if errorlevel 1 exit /b %errorlevel%
  )
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -LiteralPath '!ARCHIVE!' -DestinationPath '!DIST_DIR!' -Force"
  if errorlevel 1 exit /b %errorlevel%
)

call "%MVN_CMD%" %*
