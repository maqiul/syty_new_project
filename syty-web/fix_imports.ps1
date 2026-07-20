$dir = "I:\syty\syty-web\src\views"
Get-ChildItem -Path $dir -Recurse -Filter "*.vue" | ForEach-Object {
    $file = $_.FullName
    $content = Get-Content $file -Raw -Encoding UTF8
    
    $content = $content.Replace("../store", "@/store").Replace("../../store", "@/store")
    $content = $content.Replace("../utils", "@/utils").Replace("../../utils", "@/utils")
    $content = $content.Replace("../components", "@/components").Replace("../../components", "@/components")
    
    Set-Content $file -Value $content -Encoding UTF8 -NoNewline
}
Write-Host "Imports Fixed Again!"
