@echo off
echo Pushing code to GitHub (teobranch)...
git push --force origin teobranch
if %errorlevel% neq 0 (
    echo.
    echo ----------------------------------------------------------------
    echo Push failed! You likely need to authenticate.
    echo Please sign in via the browser popup if one appears,
    echo or ensure you have a valid token.
    echo ----------------------------------------------------------------
) else (
    echo.
    echo ----------------------------------------------------------------
    echo Push successful!
    echo Code is live at: https://github.com/TeoTzeHong/floodDectectionKitaHack/tree/teobranch
    echo ----------------------------------------------------------------
)
pause
