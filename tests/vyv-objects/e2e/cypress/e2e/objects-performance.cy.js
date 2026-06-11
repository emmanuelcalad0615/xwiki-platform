/**
 * Performance — Objetos (ObjectResourceImpl). Mide el tiempo de respuesta de la
 * API REST de objetos (res.duration en ms) contra un umbral.
 * Requisitos: docker compose up -d, XWiki en http://localhost:8080.
 */
const WIKI = 'xwiki';

describe('Performance - Objetos (ObjectResourceImpl)', () => {
  const espacio = Cypress.env('ESPACIO');
  const pagina = Cypress.env('PAGINA');
  const clase = Cypress.env('CLASE');
  const auth = {
    username: Cypress.env('XWIKI_USER'),
    password: Cypress.env('XWIKI_PASS')
  };

  const urlPagina = `/rest/wikis/${WIKI}/spaces/${espacio}/pages/${pagina}`;
  const urlObjetos = `${urlPagina}/objects`;
  const UMBRAL_MS = 2000;

  const xmlObjeto = (valorTags) =>
    `<?xml version="1.0" encoding="UTF-8"?>` +
    `<object xmlns="http://www.xwiki.org"><className>${clase}</className>` +
    `<property name="tags"><value>${valorTags}</value></property></object>`;

  let numeroObjeto;

  before(() => {
    // Arrange: pagina de trabajo con un objeto TagClass.
    cy.request({
      method: 'PUT',
      url: urlPagina,
      auth,
      headers: { 'Content-Type': 'application/xml' },
      body: `<?xml version="1.0" encoding="UTF-8"?>
<page xmlns="http://www.xwiki.org">
  <title>Perf de objetos V&amp;V</title>
  <content>Pagina creada por las pruebas de performance de objects.</content>
</page>`
    });

    cy.request({
      method: 'POST',
      url: urlObjetos,
      auth,
      headers: { 'Content-Type': 'application/xml', Accept: 'application/json' },
      body: xmlObjeto('perf')
    }).then((res) => {
      numeroObjeto = res.body.number;
    });
  });

  after(() => {
    cy.request({ method: 'DELETE', url: urlPagina, auth, failOnStatusCode: false });
  });

  it(`GET de un objeto responde en menos de ${UMBRAL_MS} ms`, () => {
    cy.request({
      url: `${urlObjetos}/${clase}/${numeroObjeto}`,
      auth,
      headers: { Accept: 'application/json' }
    }).then((res) => {
      expect(res.status).to.eq(200);
      cy.log(`GET objeto: ${res.duration} ms`);
      expect(res.duration).to.be.lessThan(UMBRAL_MS);
    });
  });

  it(`GET de la lista de objetos responde en menos de ${UMBRAL_MS} ms`, () => {
    cy.request({
      url: urlObjetos,
      auth,
      headers: { Accept: 'application/json' }
    }).then((res) => {
      expect(res.status).to.eq(200);
      cy.log(`GET lista objetos: ${res.duration} ms`);
      expect(res.duration).to.be.lessThan(UMBRAL_MS);
    });
  });

  it(`Promedio de 10 GET de objeto bajo el umbral (${UMBRAL_MS} ms)`, () => {
    const tiempos = [];
    Cypress._.times(10, () => {
      cy.request({
        url: `${urlObjetos}/${clase}/${numeroObjeto}`,
        auth,
        headers: { Accept: 'application/json' }
      }).then((res) => tiempos.push(res.duration));
    });
    cy.then(() => {
      const prom = tiempos.reduce((a, b) => a + b, 0) / tiempos.length;
      cy.log(`Promedio 10 GET objeto: ${prom.toFixed(0)} ms`);
      expect(prom).to.be.lessThan(UMBRAL_MS);
    });
  });
});
