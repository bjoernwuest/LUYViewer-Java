@echo off
echo Starting LUYViewer Debug Mode...
echo Executable: LuyViewer.exe
echo Current directory: %CD%
echo.
echo Contents of current directory:
dir /b
echo.
echo Creating data directory if it doesn't exist...
if not exist "data" mkdir data
echo.
echo Starting application with verbose output...
echo Running: LuyViewer.exe %*
LuyViewer.exe %* 2>&1
set EXIT_CODE=%ERRORLEVEL%
echo.
echo Application exited with code: %EXIT_CODE%
echo.
if exist "error.log" (
    echo Error log found:
    type error.log
    echo.
)
echo.
echo Press any key to close...
pause > nul
