# ============================================================
# correr-objects.ps1 - Funcionalidad "objects" (ObjectResourceImpl)
#
# Uso:
#   .\correr-objects.ps1                 -> unitarias + BDD en local (sin worktree)
#   .\correr-objects.ps1 unit            -> solo unitarias (proyecto-modulo-real)
#   .\correr-objects.ps1 bdd             -> solo BDD (Cucumber + Screenplay)
#   .\correr-objects.ps1 sonar -token T  -> cobertura JaCoCo + SonarQube SOLO de
#                                           ObjectResourceImpl (projectKey Xwiki-objects)
#
# El modo sonar requiere el worktree xwiki-184 (.\..\vyv-rest\setup.ps1) y un
# SonarQube en localhost:9000. Los modos locales solo necesitan JDK 17/21 y Maven.
#
# Si PowerShell bloquea el script:
#   powershell -ExecutionPolicy Bypass -File .\correr-objects.ps1
# ============================================================

param(
  [ValidateSet("todo", "unit", "bdd", "sonar")]
  [string]$modo = "todo",
  [string]$token = $env:SONAR_TOKEN,
  [string]$sonarUrl = "http://localhost:9000",
  [string]$projectKey = "Xwiki-objects"
)

$ErrorActionPreference = "Stop"

# --- Maven: usa el del PATH o cae al portable de %USERPROFILE%\tools ---
$mvnPortable = Join-Path $env:USERPROFILE "tools\apache-maven-3.9.9\bin\mvn.cmd"
if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
  if (Test-Path $mvnPortable) {
    Write-Host "mvn no esta en el PATH; usando Maven portable: $mvnPortable" -ForegroundColor Yellow
    function mvn { & $mvnPortable @args }
  }
  else {
    throw "No se encontro Maven. Instala Maven 3.9+ o deja una copia portable en $mvnPortable"
  }
}

# --- JAVA_HOME: si no esta definido, intenta con los JDK conocidos ---
if (-not $env:JAVA_HOME) {
  foreach ($jdk in @("C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot", "C:\Program Files\Java\jdk-23")) {
    if (Test-Path $jdk) { $env:JAVA_HOME = $jdk; break }
  }
}

# --- Rutas derivadas de la ubicacion del script (portables) ---
$here      = $PSScriptRoot                                # .../xwiki-platform/tests/vyv-objects
$tests     = Split-Path $here -Parent                     # .../xwiki-platform/tests
$repo      = Split-Path $tests -Parent                    # .../xwiki-platform
$pomUnit   = Join-Path $tests "vyv-rest\proyecto-modulo-real\pom.xml"
$pomBdd    = Join-Path $here  "bdd\pom.xml"
$worktree  = Join-Path (Split-Path $repo -Parent) "xwiki-184"
$modulo    = Join-Path $worktree "xwiki-platform-core\xwiki-platform-rest\xwiki-platform-rest-server"

function Invoke-Unit {
  Write-Host "==> Unitarias de ObjectResourceImpl (clase real 18.4.0)..." -ForegroundColor Cyan
  mvn test -f $pomUnit "-Dtest=ObjectResourceImplTest"
  if ($LASTEXITCODE -ne 0) { throw "Fallaron las unitarias de objects" }
}

function Invoke-Bdd {
  Write-Host "==> BDD Cucumber/Gherkin + Screenplay de objects..." -ForegroundColor Cyan
  mvn test -f $pomBdd
  if ($LASTEXITCODE -ne 0) { throw "Fallo el BDD de objects" }
}

function Invoke-SonarObjects {
  if ([string]::IsNullOrWhiteSpace($token)) {
    throw "Falta token de Sonar. Pasa -token TU_TOKEN o ejecuta antes: `$env:SONAR_TOKEN='sqp_...'"
  }
  if (-not (Test-Path $modulo)) {
    throw "Falta el worktree xwiki-184. Corre primero: ..\vyv-rest\setup.ps1"
  }
  # Sincronizar SOLO el test de objects al worktree
  $origen  = Join-Path $tests  "vyv-rest\proyecto-modulo-real\src\test\java\org\xwiki\rest\internal\resources\objects"
  $destino = Join-Path $modulo "src\test\java\org\xwiki\rest\internal\resources\objects"
  New-Item -ItemType Directory -Force -Path $destino | Out-Null
  Copy-Item "$origen\*.java" $destino -Force

  $env:MAVEN_OPTS = "-Xmx2g"
  Push-Location $modulo
  try {
    mvn clean `
      "org.jacoco:jacoco-maven-plugin:0.8.12:prepare-agent" `
      test `
      "org.jacoco:jacoco-maven-plugin:0.8.12:report" `
      "org.sonarsource.scanner.maven:sonar-maven-plugin:sonar" `
      "-Dtest=ObjectResourceImplTest" `
      "-Dsonar.projectKey=$projectKey" "-Dsonar.projectName=$projectKey" `
      "-Dsonar.inclusions=**/objects/ObjectResourceImpl.java" `
      "-Dsonar.host.url=$sonarUrl" "-Dsonar.token=$token"
    if ($LASTEXITCODE -ne 0) { throw "Fallo el analisis Sonar de objects" }
    Write-Host "==> Resultados: $sonarUrl/dashboard?id=$projectKey" -ForegroundColor Yellow
  }
  finally {
    Pop-Location
  }
}

switch ($modo) {
  "unit"  { Invoke-Unit }
  "bdd"   { Invoke-Bdd }
  "sonar" { Invoke-SonarObjects }
  default { Invoke-Unit; Invoke-Bdd; Write-Host "==> TODO VERDE (unitarias + BDD de objects)" -ForegroundColor Green }
}
