@echo off
set "DIR=%~dp0"
set "APTA="
for %%f in ("%DIR%aptasuite-*.jar") do if /I not "%%~nxf"=="aptasuite-chart-export.jar" set "APTA=%%f"
if "%APTA%"=="" (
  echo ERROR: Could not find aptasuite-x.y.z.jar in "%DIR%".
  pause
  exit /b 1
)
java %JAVA_OPTS% -cp "%APTA%;%DIR%aptasuite-chart-export.jar" chartexport.Launcher %*
