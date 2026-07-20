@echo off
:: ========================================================
:: SYTY 数据库每日自动备份脚本 (PostgreSQL + Podman)
:: ========================================================

:: 1. 设置备份目录
set "BACKUP_DIR=I:\syty\backup"
if not exist "%BACKUP_DIR%" mkdir "%BACKUP_DIR%"

:: 2. 设置文件名 (格式: syty_backup_YYYYMMDD_HHMM.sql)
for /f "tokens=2 delims==" %%a in ('wmic OS Get localdatetime /value') do set "dt=%%a"
set "YY=%dt:~2,2%" & set "YYYY=%dt:~0,4%" & set "MM=%dt:~4,2%" & set "DD=%dt:~6,2%"
set "HH=%dt:~8,2%" & set "Min=%dt:~10,2%" & set "Sec=%dt:~12,2%"
set "FILENAME=syty_backup_%YYYY%%MM%%DD%_%HH%%Min%.sql"

echo =========================================
echo 开始备份数据库 -> %BACKUP_DIR%\%FILENAME%
echo =========================================

:: 3. 执行 Podman pg_dump (使用 root 用户，密码在环境变量或交互输入)
:: 注意：如果你的容器设置了 POSTGRES_PASSWORD，这里会自动使用
podman exec postgres pg_dump -U root -d syty --clean --if-exists -f /tmp/%FILENAME%

if %errorlevel% neq 0 (
    echo [错误] Podman 备份失败!
    pause
    exit /b 1
)

:: 4. 将容器内的文件拷贝到宿主机
podman cp postgres:/tmp/%FILENAME% "%BACKUP_DIR%\%FILENAME%"

:: 5. 清理容器内临时文件
podman exec postgres rm /tmp/%FILENAME%

echo.
echo [成功] 备份完成!
echo [路径] %BACKUP_DIR%\%FILENAME%
echo.

:: 6. 删除 7 天前的旧备份 (保留一周)
forfiles /p "%BACKUP_DIR%" /m syty_backup_*.sql /d -7 /c "cmd /c del @path"
