#!/usr/bin/env bash
# ============================================================
# correr.sh  (macOS/Linux)  - equivalente a correr.ps1
# Sincroniza tests al worktree xwiki-184 y los ejecuta.
# Rutas RELATIVAS -> funciona en cualquier equipo (no depende del usuario).
#
# Uso:
#   ./correr.sh                          -> corre SOLO tus tests (comentarios + pages)
#   ./correr.sh sonar                    -> cobertura + Sonar (solo tu funcionalidad)
#   ./correr.sh equipo                   -> TODOS los tests del equipo + Sonar (cobertura combinada)
#   ./correr.sh sonar  --token TU_TOKEN
#   ./correr.sh equipo --token TU_TOKEN
#
# Requisito previo (una sola vez tras clonar/pull):  ./setup.sh
# ============================================================
set -euo pipefail

# --- Parametros ---
modo="test"
token="${SONAR_TOKEN:-}"                      # pasa --token, o export SONAR_TOKEN="..."
sonar_url="http://localhost:9000"
project_key="Xwiki"

while [ $# -gt 0 ]; do
  case "$1" in
    test|sonar|equipo) modo="$1"; shift ;;
    --token)      token="$2";       shift 2 ;;
    --sonarUrl)   sonar_url="$2";    shift 2 ;;
    --projectKey) project_key="$2";  shift 2 ;;
    *) echo "Argumento desconocido: $1"; exit 1 ;;
  esac
done

# --- Java 21 (requerido por XWiki) ---
if [ -x /usr/libexec/java_home ]; then
  if JH="$(/usr/libexec/java_home -v 21 2>/dev/null)"; then
    export JAVA_HOME="$JH"
    export PATH="$JAVA_HOME/bin:$PATH"
  fi
fi

# --- Rutas derivadas de la ubicacion del script (portables) ---
here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo="$(cd "$here/../.." && pwd)"                          # .../xwiki-platform
worktree="$(dirname "$repo")/xwiki-184"
modulo="$worktree/xwiki-platform-core/xwiki-platform-rest/xwiki-platform-rest-server"

# Arbol de tests (fuente de verdad en tu repo) y su destino en el worktree
origen_tree="$here/proyecto-modulo-real/src/test/java"
destino_tree="$modulo/src/test/java"
# Carpetas de funcionalidad (comentarios y pages)
origen_mia="$origen_tree/org/xwiki/rest/internal/resources/comments"
destino_mia="$destino_tree/org/xwiki/rest/internal/resources/comments"
origen_pages="$origen_tree/org/xwiki/rest/internal/resources/pages"
destino_pages="$destino_tree/org/xwiki/rest/internal/resources/pages"
# Codigo de produccion modificado por el equipo (sobre-escribe el de XWiki en el worktree)
origen_main="$here/proyecto-modulo-real/src/main/java"
destino_main="$modulo/src/main/java"

# --- Validaciones ---
[ -d "$modulo" ] || { echo "Falta el worktree xwiki-184. Corre primero:  ./setup.sh"; exit 1; }

# --- 1. Sincronizar tests al worktree ---
if [ "$modo" = "equipo" ]; then
  echo "==> Sincronizando TODOS los tests del equipo a xwiki-184..."
  [ -d "$origen_tree" ] || { echo "No existe el arbol de tests: $origen_tree"; exit 1; }
  # Copia recursiva (merge): agrega/actualiza sin borrar los de XWiki
  cp -R "$origen_tree/." "$destino_tree/"
else
  echo "==> Sincronizando tus tests a xwiki-184..."
  if [ -d "$origen_mia" ]; then
    mkdir -p "$destino_mia"
    cp "$origen_mia/"*.java "$destino_mia/"
  fi
  if [ -d "$origen_pages" ]; then
    mkdir -p "$destino_pages"
    cp "$origen_pages/"*.java "$destino_pages/"
  fi
  echo "    OK"
fi

# --- 1b. Sincronizar codigo de produccion modificado (si el equipo tiene src/main) ---
if [ -d "$origen_main" ]; then
  echo "==> Sincronizando codigo de produccion (main) modificado..."
  mkdir -p "$destino_main"
  cp -R "$origen_main/." "$destino_main/"
fi

# --- 2. Ejecutar ---
export MAVEN_OPTS="-Xmx2g"
cd "$modulo"

# Clases del equipo (solo estas cuentan en la cobertura cuando se usa inclusions).
team_inclusions="**/comments/Comment*Impl.java,**/objects/ObjectResourceImpl.java,**/attachments/AttachmentResourceImpl.java,**/pages/PageTagsResourceImpl.java"
# Tests del equipo a EJECUTAR (excluye los tests propios de XWiki -p.ej. AttachmentsResourceImplTest-
# que usan @OldcoreTest y rompen el component lookup en algunos entornos).
team_tests="Comment*ResourceImplTest,PageTagsResourceImpl*Test,AttachmentResourceImplTest,ObjectResourceImplTest"
mis_tests="Comment*ResourceImplTest,PageTagsResourceImpl*Test"

run_sonar() {
  local key="$1" inclusions="$2" tests="$3"
  if [ -z "$token" ]; then
    echo "Falta token de Sonar. Pasa  --token TU_TOKEN  o ejecuta antes:  export SONAR_TOKEN='sqp_...'"
    exit 1
  fi
  local args=(
    clean
    org.jacoco:jacoco-maven-plugin:0.8.12:prepare-agent
    test
    org.jacoco:jacoco-maven-plugin:0.8.12:report
    org.sonarsource.scanner.maven:sonar-maven-plugin:sonar
    "-Dtest=$tests"
    "-Dsonar.projectKey=$key" "-Dsonar.projectName=$key"
    "-Dsonar.host.url=$sonar_url" "-Dsonar.token=$token"
  )
  if [ -n "$inclusions" ]; then
    args+=("-Dsonar.inclusions=$inclusions")
  fi
  mvn "${args[@]}"
  echo "==> Resultados: $sonar_url/dashboard?id=$key"
}

case "$modo" in
  equipo)
    echo "==> Equipo: tests del equipo + cobertura combinada (solo clases del equipo) -> Sonar '$project_key'..."
    run_sonar "$project_key" "$team_inclusions" "$team_tests"
    ;;
  sonar)
    echo "==> Cobertura + Sonar '$project_key' ($sonar_url)..."
    run_sonar "$project_key" "" "$mis_tests"
    ;;
  *)
    echo "==> Corriendo tus tests..."
    mvn test "-Dtest=Comment*ResourceImplTest,PageTagsResourceImpl*Test"
    ;;
esac
