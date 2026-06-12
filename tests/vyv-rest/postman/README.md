# XWiki — Comentarios REST (Postman + IA)

Collection de pruebas del REST de **Comentarios** de XWiki (`CommentsResourceImpl`), generada con asistencia de IA.

## Archivos
- `comentarios.postman_collection.json` — la collection (7 requests con tests `pm.test`).
- `xwiki-local.postman_environment.json` — variables del entorno local.

## Requisitos
- XWiki corriendo en `http://localhost:8080` (`docker compose up -d` en la raíz del repo).
- Usuario admin `emmanuel_calad` / `Joaco06151970_` (ya configurado).

## Correr en la app de Postman
1. **Import** → arrastra los 2 archivos `.json`.
2. Selecciona el environment **XWiki Local** (arriba a la derecha).
3. **Run collection** (Runner). Corre los 7 requests en orden.

> El orden importa: el request *2. Crear* guarda `commentId` / `commentText` en variables, que usan *3. Obtener por id* y *5. Verificar*.

## Correr por CLI (Newman)
```bash
npm i -g newman
cd tests/vyv-rest/postman
newman run comentarios.postman_collection.json -e xwiki-local.postman_environment.json
```

## Cobertura (espeja unit tests + Cypress)
| # | Request | Valida |
|---|---|---|
| 1 | Listar comentarios | 200 + `comments[]` |
| 2 | Crear comentario | 201 + eco de `text` + autor; guarda `commentId` |
| 3 | Obtener por id | 200 + id/text coinciden |
| 4 | Crear con highlight | 201 + `highlight` guardado |
| 5 | Listar y verificar | el comentario creado aparece en la lista |
| 6 | Seguridad: sin auth | 401 |
| 7 | Seguridad: credenciales inválidas | 401 |

## IA dentro de Postman (Postbot)
Abre cualquier request → icono **Postbot** (IA) y pide, por ejemplo:
- "Genera tests que validen el status y el schema JSON de la respuesta."
- "Documenta esta request con ejemplos."
- "Crea casos borde (texto vacío, payload muy largo, caracteres XSS)."

Postbot escribe los `pm.test` en la pestaña **Tests** y la documentación automáticamente.

## Endpoints (referencia)
```
GET    {{baseUrl}}/rest/wikis/{{wiki}}/spaces/{{space}}/pages/{{page}}/comments
POST   {{baseUrl}}/rest/wikis/{{wiki}}/spaces/{{space}}/pages/{{page}}/comments      (Basic auth)
GET    {{baseUrl}}/rest/wikis/{{wiki}}/spaces/{{space}}/pages/{{page}}/comments/{id}
```
