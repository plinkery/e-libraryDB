@echo off
:: ===================================================================
:: WINDOWS PORT FORWARDING HELPER FOR WSL2
:: Allows mobile phones on Wi-Fi / Hotspot to connect to E-Library
:: ===================================================================

:: Check for Administrator privileges; if not elevated, request UAC elevation
net session >nul 2>&1
if %errorLevel% neq 0 (
    echo =============================================================
    echo [INFO] Requesting Administrator privileges...
    echo Please click 'Yes' on the Windows prompt.
    echo =============================================================
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Start-Process -FilePath '%~f0' -Verb RunAs"
    exit /b
)

title E-Library Mobile Hotspot & Wi-Fi Port Forwarding
echo =============================================================
echo   SETTING UP MOBILE HOTSPOT / WI-FI PORT FORWARDING FOR WSL2
echo =============================================================
echo.

:: Detect WSL IP
for /f "tokens=1" %%a in ('wsl hostname -I') do set WSL_IP=%%a

if "%WSL_IP%"=="" (
    echo [ERROR] Could not detect WSL2 IP address. Make sure WSL is running!
    pause
    exit /b
)

echo [OK] Detected WSL2 Internal IP: %WSL_IP%
echo [OK] Setting up Windows Port 8080 -^> %WSL_IP%:8080...

:: Remove any old rule and add fresh forwarding rule
netsh interface portproxy delete v4tov4 listenport=8080 listenaddress=0.0.0.0 >nul 2>&1
netsh interface portproxy add v4tov4 listenport=8080 listenaddress=0.0.0.0 connectport=8080 connectaddress=%WSL_IP%

:: Open Windows Firewall for Port 8080 across all profiles (Public and Private)
netsh advfirewall firewall delete rule name="ELibrary_WSL_8080" >nul 2>&1
netsh advfirewall firewall add rule name="ELibrary_WSL_8080" dir=in action=allow protocol=TCP localport=8080 profile=any >nul 2>&1

echo.
echo =============================================================
echo   VERIFYING PORT FORWARDING STATUS:
echo =============================================================
netsh interface portproxy show v4tov4
echo =============================================================

echo.
echo =============================================================
echo   USE ONE OF THE URLS BELOW ON YOUR PHONE'S BROWSER:
echo =============================================================
powershell -NoProfile -Command "Get-NetIPAddress -AddressFamily IPv4 | Where-Object { $_.InterfaceAlias -notlike '*vEthernet*' -and $_.InterfaceAlias -notlike '*VirtualBox*' -and $_.IPAddress -notlike '127.*' -and $_.IPAddress -notlike '169.254*' } | ForEach-Object { Write-Host ('  - [' + $_.InterfaceAlias + ']: http://' + $_.IPAddress + ':8080') -ForegroundColor Green }"
echo =============================================================
echo.
echo IMPORTANT NOTES:
echo 1. Do NOT use 172.x.x.x (that is Windows internal WSL switch).
echo 2. If your phone says 'took too long to respond':
echo    - Make sure phone is on the SAME Wi-Fi network.
echo    - If you have McAfee/Norton/AVG antivirus: temporarily pause
echo      its Firewall (third-party firewalls block incoming connections).
echo    - Or turn on 'Mobile Hotspot' in Windows Settings and connect
echo      your phone directly to your laptop's hotspot!
echo.
pause
