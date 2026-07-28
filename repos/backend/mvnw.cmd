@echo off
setlocal
set "_MVN=D:\dev-tools\apache-maven-3.9.16-bin\apache-maven-3.9.16\bin\mvn.cmd"
if not exist "%_MVN%" if not "%MAVEN_HOME%"=="" set "_MVN=%MAVEN_HOME%\bin\mvn.cmd"
if not exist "%_MVN%" (
  echo Maven 3.9+ was not found. Set MAVEN_HOME to a Maven 3.9+ installation.
  exit /b 1
)
"%_MVN%" %*
