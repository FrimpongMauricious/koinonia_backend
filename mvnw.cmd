@REM ----------------------------------------------------------------------------
@REM Apache Maven Wrapper startup batch script, version 3.3.2
@REM ----------------------------------------------------------------------------
@echo off
setlocal enabledelayedexpansion

set "BASE_DIR=%~dp0"
set "BASE_DIR=%BASE_DIR:~0,-1%"

set "WRAPPER_PROPERTIES=%BASE_DIR%\.mvn\wrapper\maven-wrapper.properties"

for /f "tokens=2 delims==" %%a in ('findstr /i "distributionUrl" "%WRAPPER_PROPERTIES%"') do (
    set "DISTRIBUTION_URL=%%a"
)

for %%f in ("%DISTRIBUTION_URL%") do set "MAVEN_DIST_FILENAME=%%~nxf"
set "MAVEN_DIST_NAME=%MAVEN_DIST_FILENAME:.zip=%"

if not defined MAVEN_USER_HOME set "MAVEN_USER_HOME=%USERPROFILE%\.m2"
set "MAVEN_DIST_DIR=%MAVEN_USER_HOME%\wrapper\dists\%MAVEN_DIST_NAME%"
set "MAVEN_HOME=%MAVEN_DIST_DIR%\%MAVEN_DIST_NAME%"

if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
    echo Downloading %DISTRIBUTION_URL%
    if not exist "%MAVEN_DIST_DIR%" mkdir "%MAVEN_DIST_DIR%"
    powershell -Command "Invoke-WebRequest -Uri '%DISTRIBUTION_URL%' -OutFile '%MAVEN_DIST_DIR%\download.zip'"
    powershell -Command "Expand-Archive -Path '%MAVEN_DIST_DIR%\download.zip' -DestinationPath '%MAVEN_DIST_DIR%' -Force"
    del "%MAVEN_DIST_DIR%\download.zip"
)

"%MAVEN_HOME%\bin\mvn.cmd" %*
