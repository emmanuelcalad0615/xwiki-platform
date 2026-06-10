// cypress/support/e2e.js
// Import commands.js using ES2015 syntax:
import './commands'

// Ocultar errores no capturados de la aplicación para que no fallen los tests por scripts de terceros en XWiki
Cypress.on('uncaught:exception', (err, runnable) => {
  return false;
});
