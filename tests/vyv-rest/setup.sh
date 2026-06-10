#!/usr/bin/env bash
# ============================================================
# setup.sh  -  CORRER UNA SOLA VEZ tras clonar/pull el repo. (macOS/Linux)
# Equivalente a setup.ps1 (Windows).
#
# Deja todo listo para compilar/probar XWiki sin construir el monorepo:
#   1. Configura Maven con el repo de XWiki (nexus)  -> ~/.m2/settings.xml
#   2. Baja el tag release 18.4.0 de XWiki oficial
#   3. Crea el worktree  ../xwiki-184  (donde SI compila)
#
# Uso:
#   chmod +x setup.sh   # una sola vez
#   ./setup.sh
# ============================================================
set -euo pipefail

here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo="$(cd "$here/../.." && pwd)"            # .../xwiki-platform
parent="$(dirname "$repo")"
worktree="$parent/xwiki-184"
tag="xwiki-platform-18.4.0"

echo "Repo:     $repo"
echo "Worktree: $worktree"
echo

# --- 1. settings.xml global (repos nexus de XWiki) ---
m2="$HOME/.m2"
mkdir -p "$m2"
settings_dst="$m2/settings.xml"
if [ ! -f "$settings_dst" ]; then
  cp "$here/settings-xwiki.xml" "$settings_dst"
  echo "[1/3] settings.xml global creado."
else
  echo "[1/3] Ya existe ~/.m2/settings.xml (no lo piso)."
  echo "      Si Maven no encuentra plugins de XWiki, fusiona el contenido de settings-xwiki.xml."
fi

# --- 2. Bajar el tag release si falta ---
cd "$repo"
if [ -z "$(git tag --list "$tag")" ]; then
  echo "[2/3] Bajando tag $tag de XWiki oficial (puede tardar)..."
  git fetch --depth 1 https://github.com/xwiki/xwiki-platform.git "refs/tags/${tag}:refs/tags/${tag}"
  echo "[2/3] Tag bajado."
else
  echo "[2/3] Tag $tag ya presente."
fi

# --- 3. Crear worktree si falta ---
if [ ! -d "$worktree" ]; then
  echo "[3/3] Creando worktree (checkout de 14k archivos, espera)..."
  git worktree add "$worktree" "$tag"
  echo "[3/3] Worktree creado."
else
  echo "[3/3] Worktree ya existe: $worktree"
fi

echo
echo "LISTO. Ahora puedes:"
echo "   ./correr.sh                         # corre tus tests"
echo "   ./correr.sh sonar --token sqp_...   # cobertura + Sonar"
echo
echo "(La primera compilacion baja dependencias a ~/.m2, tarda unos minutos.)"
