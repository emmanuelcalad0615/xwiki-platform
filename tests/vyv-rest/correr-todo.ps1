# ============================================================
# correr-todo.ps1  -  Ejecuta y REGISTRA toda la bateria de pruebas del equipo
# (las 4 funcionalidades: comentarios, etiquetas, objetos, attachments) en una
# sola corrida, dejando un resumen consolidado.
#
# Cubre:
#   1. Unitarias JUnit 5 + Mockito de TODO el modulo rest-server (mini-pom)
#   2. BDD Cucumber/Gherkin/Screenplay de etiquetas+comentarios (vyv-rest/bdd)
#   3. BDD Cucumber/Gherkin/Screenplay de objetos (vyv-objects/bdd)
#   4. Cypress del proyecto compartido: api, security, performance, a11y, ui,
#      regression para las 3 funcionalidades (requiere XWiki en localhost:8080)
#   5. Playwright de objetos (requiere XWiki)
#   6. IA (DeepEval + Stagehand + generador) de objetos -- solo si hay GROQ_API_KEY
#
# Uso:
#   .\correr-todo.ps1                       -> unitarias + BDD (no requiere Docker)
#   .\correr-todo.ps1 -e2e                  -> agrega Cypress + Playwright (XWiki arriba)
#   .\correr-todo.ps1 -e2e -ia              -> agrega ademas las pruebas de IA con Groq
#   .\correr-todo.ps1 -e2e -xwikiUser Admin -xwikiPass admin
#
# Si PowerShell bloquea el script:
#   powershell -ExecutionPolicy Bypass -File .\correr-todo.ps1 -e2e
# ============================================================

param(
  [switch]$e2e,
  [switch]$ia,
  [string]$xwikiUser = "Admin",
  [string]$xwikiPass = "admin"
)

$ErrorActionPreference = "Continue"

# --- Maven: usa el del PATH o cae al portable de %USERPROFILE%\tools ---
$mvnPortable = Join-Path $env:USERPROFILE "tools\apache-maven-3.9.9\bin\mvn.cmd"
if (Get-Command mvn -ErrorAction SilentlyContinue) {
  $mvn = "mvn"
} elseif (Test-Path $mvnPortable) {
  $mvn = $mvnPortable
  Write-Host "mvn no esta en el PATH; usando Maven portable: $mvnPortable" -ForegroundColor Yellow
} else {
  throw "No se encontro Maven. Instala Maven 3.9+ o deja una copia portable en $mvnPortable"
}

if (-not $env:JAVA_HOME) {
  foreach ($jdk in @("C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot", "C:\Program Files\Java\jdk-23")) {
    if (Test-Path $jdk) { $env:JAVA_HOME = $jdk; break }
  }
}

$here  = $PSScriptRoot                                # .../tests/vyv-rest
$tests = Split-Path $here -Parent                     # .../tests
$unitPom = Join-Path $here  "proyecto-modulo-real\pom.xml"
$bddTags = Join-Path $here  "bdd\pom.xml"
$bddObj  = Join-Path $tests "vyv-objects\bdd\pom.xml"
$cypress = Join-Path $here  "cypress"
$pwObj   = Join-Path $tests "vyv-objects\playwright"

$resultados = [System.Collections.ArrayList]::new()
function Registrar([string]$nombre, [int]$code) {
  $estado = if ($code -eq 0) { "OK" } else { "FALLO" }
  [void]$resultados.Add([pscustomobject]@{ Suite = $nombre; Estado = $estado; Exit = $code })
  $color = if ($code -eq 0) { "Green" } else { "Red" }
  Write-Host "==> [$estado] $nombre" -ForegroundColor $color
}

# --- 1. Unitarias de TODO el modulo (las 4 funcionalidades + tests propios de XWiki) ---
Write-Host "`n=== 1/6  Unitarias JUnit 5 (modulo rest-server completo) ===" -ForegroundColor Cyan
& $mvn -q -B test -f $unitPom "-Dtest=Comment*ResourceImplTest,PageTagsResourceImpl*Test,AttachmentResourceImplTest,ObjectResourceImplTest"
Registrar "Unitarias (comentarios+etiquetas+objetos+attachments)" $LASTEXITCODE

# --- 2. BDD etiquetas + comentarios ---
Write-Host "`n=== 2/6  BDD Cucumber/Screenplay (etiquetas + comentarios) ===" -ForegroundColor Cyan
& $mvn -q -B test -f $bddTags
Registrar "BDD etiquetas+comentarios" $LASTEXITCODE

# --- 3. BDD objetos ---
Write-Host "`n=== 3/6  BDD Cucumber/Screenplay (objetos) ===" -ForegroundColor Cyan
& $mvn -q -B test -f $bddObj
Registrar "BDD objetos" $LASTEXITCODE

if ($e2e) {
  # --- 4. Cypress del proyecto compartido (6 categorias x 3 funcionalidades) ---
  Write-Host "`n=== 4/6  Cypress compartido: etiquetas + comentarios + objetos ===" -ForegroundColor Cyan
  Push-Location $cypress
  if (-not (Test-Path node_modules)) { npm install --no-audit --no-fund | Out-Null }
  $envArg = "adminUser=$xwikiUser,adminPass=$xwikiPass"
  npx cypress run --env $envArg
  Registrar "Cypress compartido (todas las funcionalidades)" $LASTEXITCODE
  Pop-Location

  # --- 5. Playwright de objetos ---
  Write-Host "`n=== 5/6  Playwright (objetos) ===" -ForegroundColor Cyan
  Push-Location $pwObj
  if (-not (Test-Path node_modules)) { npm install --no-audit --no-fund | Out-Null }
  $env:XWIKI_USER = $xwikiUser
  $env:XWIKI_PASS = $xwikiPass
  $shell = "$env:LOCALAPPDATA\ms-playwright\chromium_headless_shell-1223\chrome-headless-shell-win64\chrome-headless-shell.exe"
  if ((-not $env:PW_CHROME_PATH) -and (Test-Path $shell)) { $env:PW_CHROME_PATH = $shell }
  npx playwright test
  Registrar "Playwright objetos" $LASTEXITCODE
  Pop-Location
} else {
  Write-Host "`n(omitiendo Cypress y Playwright: pasa -e2e con XWiki en localhost:8080)" -ForegroundColor DarkGray
}

if ($e2e -and $ia) {
  # --- 6. IA de objetos (DeepEval + generador) -- requiere GROQ_API_KEY ---
  Write-Host "`n=== 6/6  IA (DeepEval + generador) de objetos ===" -ForegroundColor Cyan
  if ([string]::IsNullOrWhiteSpace($env:GROQ_API_KEY)) {
    Write-Host "    (omitido: exporta `$env:GROQ_API_KEY para las pruebas de IA)" -ForegroundColor DarkGray
  } else {
    $py = "$env:USERPROFILE\tools\python-nuget\tools\python.exe"
    if (-not (Test-Path $py)) { $py = "python" }
    $env:XWIKI_USER = $xwikiUser; $env:XWIKI_PASS = $xwikiPass; $env:DEEPEVAL_TELEMETRY_OPT_OUT = "YES"
    Push-Location (Join-Path $tests "vyv-objects\ai\deepeval")
    & $py -m pytest -q
    Registrar "IA DeepEval objetos" $LASTEXITCODE
    Pop-Location
  }
}

# --- Resumen consolidado ---
Write-Host "`n================= RESUMEN =================" -ForegroundColor Cyan
$resultados | Format-Table -AutoSize
$fallos = ($resultados | Where-Object Estado -eq "FALLO").Count
if ($fallos -eq 0) {
  Write-Host "TODO VERDE: $($resultados.Count) suites ejecutadas, 0 fallos." -ForegroundColor Green
  exit 0
} else {
  Write-Host "$fallos suite(s) con fallos de $($resultados.Count)." -ForegroundColor Red
  exit 1
}
