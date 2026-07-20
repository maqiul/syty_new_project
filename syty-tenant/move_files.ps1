$views = "I:\syty\syty-web\src\views"

# Tenant
Move-Item "$views\auth\Login.vue" "$views\tenant\auth\" -Force
Move-Item "$views\auth\CustomerRegister.vue" "$views\tenant\auth\" -Force
Move-Item "$views\auth\TennisCustomerRegister.vue" "$views\tenant\auth\" -Force
Move-Item "$views\dashboard\Dashboard.vue" "$views\tenant\dashboard\" -Force
Move-Item "$views\Finance.vue" "$views\tenant\" -Force
Move-Item "$views\Stock.vue" "$views\tenant\inventory\" -Force
Move-Item "$views\order\*.*" "$views\tenant\orders\" -Force
Move-Item "$views\stringing\*.*" "$views\tenant\stringing\" -Force
Move-Item "$views\assets\Shop.vue" "$views\tenant\assets\" -Force
Move-Item "$views\assets\ShopStrings.vue" "$views\tenant\assets\" -Force
Move-Item "$views\assets\Tennis*.vue" "$views\tenant\assets\" -Force

# Admin
Move-Item "$views\auth\AdminLogin.vue" "$views\admin\login\" -Force
Move-Item "$views\dashboard\AdminDashboard.vue" "$views\admin\dashboard\" -Force
Move-Item "$views\system\Tenant.vue" "$views\admin\tenants\" -Force
Move-Item "$views\system\User.vue" "$views\admin\users\" -Force
Move-Item "$views\system\Role.vue" "$views\admin\users\" -Force
Move-Item "$views\system\MenuManage.vue" "$views\admin\users\" -Force
Move-Item "$views\system\PermManage.vue" "$views\admin\users\" -Force
Move-Item "$views\system\OperationLog.vue" "$views\admin\users\" -Force
Move-Item "$views\assets\Racket.vue" "$views\admin\assets\" -Force
Move-Item "$views\assets\Player.vue" "$views\admin\assets\" -Force
Move-Item "$views\assets\StringInfo.vue" "$views\admin\assets\" -Force

Write-Host "Moved successfully"
