$env:JAVA_HOME = "C:\Program Files\Java\jdk-24"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$env:JAVA_TOOL_OPTIONS = "-Duser.timezone=Asia/Kolkata"
docker start restaurant-db | Out-Null
Start-Sleep -Seconds 4
$p = (Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue).OwningProcess; if ($p) { Stop-Process -Id $p -Force }
.\mvnw.cmd spring-boot:run
