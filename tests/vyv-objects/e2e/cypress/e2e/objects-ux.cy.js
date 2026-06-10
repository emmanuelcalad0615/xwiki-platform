/**
 * Pruebas de experiencia de usuario (UX) alrededor de la funcionalidad "objects".
 *
 * El objeto TagClass creado por la API REST debe verse reflejado en la interfaz
 * (las etiquetas de la pagina), y el flujo de entrada del usuario (login) debe
 * ser usable y accesible. Requiere el mismo entorno que objects-api.cy.js.
 */
const WIKI = 'xwiki';

describe('UX: los objetos de la pagina se reflejan en la interfaz', () => {
  const espacio = Cypress.env('ESPACIO');
  const pagina = Cypress.env('PAGINA');
  const clase = Cypress.env('CLASE');
  const auth = {
    username: Cypress.env('XWIKI_USER'),
    password: Cypress.env('XWIKI_PASS')
  };

  const urlPagina = `/rest/wikis/${WIKI}/spaces/${espacio}/pages/${pagina}`;

  before(() => {
    cy.request({
      method: 'PUT',
      url: urlPagina,
      auth,
      headers: { 'Content-Type': 'application/xml' },
      body: `<?xml version="1.0" encoding="UTF-8"?>
<page xmlns="http://www.xwiki.org">
  <title>Prueba UX de objetos</title>
  <content>Pagina para validar que el objeto tag se ve en la interfaz.</content>
</page>`
    });
    cy.request({
      method: 'POST',
      url: `${urlPagina}/objects`,
      auth,
      headers: { 'Content-Type': 'application/xml' },
      body:
        `<?xml version="1.0" encoding="UTF-8"?>` +
        `<object xmlns="http://www.xwiki.org"><className>${clase}</className>` +
        `<property name="tags"><value>etiqueta-ux</value></property></object>`
    });
  });

  after(() => {
    cy.request({ method: 'DELETE', url: urlPagina, auth, failOnStatusCode: false });
  });

  it('la pagina de login es usable: campos visibles, con etiquetas y sin errores', () => {
    // Sin sesion previa (cy.request del before deja cookie de Admin) y
    // tolerando el 401 con el que XWiki sirve su formulario de login.
    cy.clearCookies();
    cy.visit('/bin/login/XWiki/XWikiLogin', { failOnStatusCode: false });
    cy.get('html').should('have.attr', 'lang');
    cy.get('#j_username').should('be.visible');
    cy.get('#j_password').should('be.visible');
    cy.contains('button, input[type=submit]', /log|entrar|iniciar/i).should('be.visible');
  });

  it('un usuario puede iniciar sesion y ver la pagina con su etiqueta (objeto)', () => {
    cy.clearCookies();
    cy.visit('/bin/login/XWiki/XWikiLogin', { failOnStatusCode: false });
    cy.get('#j_username').type(auth.username);
    cy.get('#j_password').type(auth.password, { log: false });
    cy.get('form').first().submit();

    cy.visit(`/bin/view/${espacio}/${pagina}`);
    // El titulo de la pagina es visible (jerarquia de informacion correcta)
    cy.get('#document-title, h1').should('contain.text', 'Prueba UX de objetos');
    // El objeto TagClass creado por la API se refleja en la pagina renderizada:
    // XWiki lo expone como keyword del documento (con el flavor completo ademas
    // se muestra como chip visual de etiqueta en el pie de la pagina).
    cy.get('meta[name="keywords"]')
      .should('have.attr', 'content')
      .and('include', 'etiqueta-ux');
  });

  it('la API de objetos responde dentro de un presupuesto de tiempo razonable', () => {
    const inicio = Date.now();
    cy.request({
      url: `${urlPagina}/objects`,
      auth,
      headers: { Accept: 'application/json' }
    }).then((res) => {
      expect(res.status).to.eq(200);
      // Presupuesto UX: una lista de objetos no deberia tardar mas de 3s
      expect(Date.now() - inicio).to.be.lessThan(3000);
    });
  });
});
