# ============================================================
# correr.ps1
# Sincroniza tests al worktree xwiki-184 y los ejecuta.
# Rutas RELATIVAS -> funciona en cualquier PC (no depende del usuario).
#
# Uso:
#   .\correr.ps1                          -> corre SOLO tus tests (comentarios)
#   .\correr.ps1 sonar                    -> cobertura + Sonar (solo tu funcionalidad)
#   .\correr.ps1 equipo                   -> TODOS los tests del equipo + Sonar (cobertura combinada)
#   .\correr.ps1 sonar  -token TU_TOKEN
#   .\correr.ps1 equipo -token TU_TOKEN
#
# Requisito previo (una sola vez tras clonar/pull):  .\setup.ps1
#
# Si PowerShell bloquea el script:
#   powershell -ExecutionPolicy Bypass -File .\correr.ps1 equipo
# ============================================================

param(
  [ValidateSet("test", "sonar", "equipo")]
  [string]$modo = "test",
  [string]$token = $env:SONAR_TOKEN,        # pasa -token, o set $env:SONAR_TOKEN="..."
  [string]$sonarUrl = "http://localhost:9000",
  [string]$projectKey = "Xwiki"             # en modo 'sonar' individual conviene uno propio
)

$ErrorActionPreference = "Stop"

# --- Rutas derivadas de la ubicacion del script (portables) ---
$here        = $PSScriptRoot
$repo        = (Resolve-Path "$here\..\..").Path          # .../xwiki-platform
$worktree    = Join-Path (Split-Path $repo -Parent) "xwiki-184"
$modulo      = Join-Path $worktree "xwiki-platform-core\xwiki-platform-rest\xwiki-platform-rest-server"

# Arbol de tests (fuente de verdad en tu repo) y su destino en el worktree
$origenTree  = Join-Path $here   "proyecto-modulo-real\src\test\java"
$destinoTree = Join-Path $modulo "src\test\java"
# Carpeta de TU funcionalidad (comentarios)
$origenMia   = Join-Path $origenTree  "org\xwiki\rest\internal\resources\comments"
$destinoMia  = Join-Path $destinoTree "org\xwiki\rest\internal\resources\comments"

# --- Validaciones ---
if (-not (Test-Path $modulo)) { throw "Falta el worktree xwiki-184. Corre primero:  .\setup.ps1" }

# --- 1. Sincronizar tests al worktree ---
if ($modo -eq "equipo") {
  Write-Host "==> Sincronizando TODOS los tests del equipo a xwiki-184..." -ForegroundColor Cyan
  if (-not (Test-Path $origenTree)) { throw "No existe el arbol de tests: $origenTree" }
  # Copia recursiva (merge): agrega/actualiza los tests del equipo sin borrar los de XWiki
  Copy-Item "$origenTree\*" $destinoTree -Recurse -Force
}
else {
  Write-Host "==> Sincronizando tus tests (comentarios) a xwiki-184..." -ForegroundColor Cyan
  if (-not (Test-Path $origenMia)) { throw "No existe el origen de tests: $origenMia" }
  New-Item -ItemType Directory -Force -Path $destinoMia | Out-Null
  Copy-Item "$origenMia\*.java" $destinoMia -Force
}
Write-Host "    OK" -ForegroundColor Green

# --- 2. Ejecutar ---
$env:MAVEN_OPTS = "-Xmx2g"
Set-Location $modulo

# Clases del equipo (solo estas cuentan en la cobertura cuando se usa $inclusions).
# Ajusta esta lista si el equipo cambia de funcionalidades.
$teamInclusions = "**/comments/Comment*Impl.java,**/objects/ObjectResourceImpl.java,**/attachments/AttachmentResourceImpl.java,**/pages/PageTagsResourceImpl.java"

function Invoke-Sonar([string]$key, [string]$inclusions) {
  if ([string]::IsNullOrWhiteSpace($token)) {
    throw "Falta token de Sonar. Pasa  -token TU_TOKEN  o ejecuta antes:  `$env:SONAR_TOKEN='sqp_...'"
  }
  $mvnArgs = @(
    "clean",
    "org.jacoco:jacoco-maven-plugin:0.8.12:prepare-agent",
    "test",
    "org.jacoco:jacoco-maven-plugin:0.8.12:report",
    "org.sonarsource.scanner.maven:sonar-maven-plugin:sonar",
    "-Dsonar.projectKey=$key", "-Dsonar.projectName=$key",
    "-Dsonar.host.url=$sonarUrl", "-Dsonar.token=$token"
  )
  if (-not [string]::IsNullOrWhiteSpace($inclusions)) {
    $mvnArgs += "-Dsonar.inclusions=$inclusions"
  }
  mvn @mvnArgs
  Write-Host "==> Resultados: $sonarUrl/dashboard?id=$key" -ForegroundColor Yellow
}

switch ($modo) {
  "equipo" {
    Write-Host "==> Equipo: TODOS los tests + cobertura combinada (solo clases del equipo) -> Sonar '$projectKey'..." -ForegroundColor Cyan
    Invoke-Sonar $projectKey $teamInclusions   # un solo analisis, cobertura limitada a las clases del equipo
  }
  "sonar" {
    Write-Host "==> Cobertura + Sonar '$projectKey' ($sonarUrl)..." -ForegroundColor Cyan
    Invoke-Sonar $projectKey $null             # tu funcionalidad, sin filtro de inclusiones
  }
  default {
    Write-Host "==> Corriendo tus tests..." -ForegroundColor Cyan
    mvn test "-Dtest=Comment*ResourceImplTest"
  }
}
