@echo off
setlocal enabledelayedexpansion

cd /d F:\syti_new_project\syty-web\src

for /r %%f in (*.ts *.vue) do (
    powershell -Command "(Get-Content '%%f' -Raw) -replace 'ant-design-vue/es/locale/zh_CN', 'antdv-next/es/locale/zh_CN' -replace 'ant-design-vue/dist/reset.css', 'antdv-next/dist/reset.css' -replace 'ant-design-vue', 'antdv-next' -replace '@ant-design/icons-vue', '@antdv-next/icons' | Set-Content '%%f' -NoNewline"
)

echo Done!
