const { defineConfig } = require('cypress');

module.exports = defineConfig({
  e2e: {
    // XWiki del docker-compose de la raiz del repo (docker compose up -d)
    baseUrl: 'http://localhost:8080',
    supportFile: false,
    video: false,
    defaultCommandTimeout: 20000,
    requestTimeout: 30000,
    responseTimeout: 30000,
    env: {
      // Credenciales del usuario administrador creado al instalar el flavor.
      // Se pueden sobreescribir: npx cypress run --env XWIKI_USER=...,XWIKI_PASS=...
      XWIKI_USER: 'emmanuel_calad',
      XWIKI_PASS: 'Joaco06151970_',
      // Pagina de trabajo exclusiva de estas pruebas (no toca contenido ajeno)
      ESPACIO: 'VyVObjects',
      PAGINA: 'PruebaObjetos',
      CLASE: 'XWiki.TagClass'
    }
  }
});
