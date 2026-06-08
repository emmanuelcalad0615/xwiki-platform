# ============================================================
# setup.ps1  -  CORRER UNA SOLA VEZ tras clonar/pull el repo.
#
# Deja todo listo para compilar/probar XWiki sin construir el monorepo:
#   1. Configura Maven con el repo de XWiki (nexus)  -> ~/.m2/settings.xml
#   2. Baja el tag release 18.4.0 de XWiki oficial
#   3. Crea el worktree  ../xwiki-184  (donde SI compila)
#
# Uso:
#   .\setup.ps1
#   (si PowerShell bloquea)  powershell -ExecutionPolicy Bypass -File .\setup.ps1
# ============================================================

$ErrorActionPreference = "Stop"

$here     = $PSScriptRoot
$repo     = (Resolve-Path "$here\..\..").Path           # .../xwiki-platform
$parent   = Split-Path $repo -Parent
$worktree = Join-Path $parent "xwiki-184"
$tag      = "xwiki-platform-18.4.0"

Write-Host "Repo:     $repo"
Write-Host "Worktree: $worktree`n"

# --- 1. settings.xml global (repos nexus de XWiki) ---
$m2 = Join-Path $env:USERPROFILE ".m2"
New-Item -ItemType Directory -Force -Path $m2 | Out-Null
$settingsDst = Join-Path $m2 "settings.xml"
if (-not (Test-Path $settingsDst)) {
  Copy-Item (Join-Path $here "settings-xwiki.xml") $settingsDst
  Write-Host "[1/3] settings.xml global creado." -ForegroundColor Green
}
else {
  Write-Host "[1/3] Ya existe ~/.m2/settings.xml (no lo piso)." -ForegroundColor Yellow
  Write-Host "      Si Maven no encuentra plugins de XWiki, fusiona el contenido de settings-xwiki.xml." -ForegroundColor Yellow
}

# --- 2. Bajar el tag release si falta ---
Set-Location $repo
$hasTag = git tag --list $tag
if (-not $hasTag) {
  Write-Host "[2/3] Bajando tag $tag de XWiki oficial (puede tardar)..." -ForegroundColor Cyan
  git fetch --depth 1 https://github.com/xwiki/xwiki-platform.git "refs/tags/${tag}:refs/tags/${tag}"
  Write-Host "[2/3] Tag bajado." -ForegroundColor Green
}
else {
  Write-Host "[2/3] Tag $tag ya presente." -ForegroundColor Yellow
}

# --- 3. Crear worktree si falta ---
if (-not (Test-Path $worktree)) {
  Write-Host "[3/3] Creando worktree (checkout de 14k archivos, espera)..." -ForegroundColor Cyan
  git worktree add $worktree $tag
  Write-Host "[3/3] Worktree creado." -ForegroundColor Green
}
else {
  Write-Host "[3/3] Worktree ya existe: $worktree" -ForegroundColor Yellow
}

Write-Host "`nLISTO. Ahora puedes:" -ForegroundColor Green
Write-Host "   .\correr.ps1                       # corre tus tests"
Write-Host "   .\correr.ps1 sonar -token sqp_...  # cobertura + Sonar"
Write-Host "`n(La primera compilacion baja dependencias a ~/.m2, tarda unos minutos.)"
