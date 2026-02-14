@echo off
echo Pulling latest code from GitHub (teobranch)...
git pull origin teobranch
if %errorlevel% neq 0 (
    echo.
    echo ----------------------------------------------------------------
    echo Pull failed! If you have uncommitted changes, commit or stash them first.
    echo You may also need to authenticate.
    echo ----------------------------------------------------------------
) else (
    echo.
    echo ----------------------------------------------------------------
    echo Pull successful! Your local code is up to date.
    echo ----------------------------------------------------------------
)
pause
