/**
 * E2E de la funcionalidad "objects" (ObjectResourceImpl) contra un XWiki REAL.
 *
 * Ciclo de vida completo del recurso REST de objetos de una pagina:
 *   crear pagina -> crear objeto -> GET (getObject) -> PUT (updateObject)
 *   -> DELETE (deleteObject) -> verificaciones negativas (404 / 401).
 *
 * Requisitos:
 *   - docker compose up -d   (en la raiz del repo)
 *   - XWiki instalado (flavor) en http://localhost:8080, usuario Admin/admin
 *
 * Cada caso espeja un camino ya probado en unitarias y BDD:
 *   200 exito | 404 objeto inexistente | 401 sin permiso de edicion | 202 update | 204 delete
 */
const WIKI = 'xwiki';

describe('API REST de objetos de una pagina (ObjectResourceImpl)', () => {
  const espacio = Cypress.env('ESPACIO');
  const pagina = Cypress.env('PAGINA');
  const clase = Cypress.env('CLASE');
  const auth = {
    username: Cypress.env('XWIKI_USER'),
    password: Cypress.env('XWIKI_PASS')
  };

  const urlPagina = `/rest/wikis/${WIKI}/spaces/${espacio}/pages/${pagina}`;
  const urlObjetos = `${urlPagina}/objects`;

  // Representacion XML del objeto: XWiki exige XML/JSON (no form-urlencoded,
  // que bloquea su proteccion CSRF del API REST).
  const xmlObjeto = (valorTags) =>
    `<?xml version="1.0" encoding="UTF-8"?>` +
    `<object xmlns="http://www.xwiki.org"><className>${clase}</className>` +
    `<property name="tags"><value>${valorTags}</value></property></object>`;

  let numeroObjeto; // numero asignado por XWiki al crear el objeto

  before(() => {
    // Arrange global: la pagina de trabajo existe y tiene un objeto TagClass.
    cy.request({
      method: 'PUT',
      url: urlPagina,
      auth,
      headers: { 'Content-Type': 'application/xml' },
      body: `<?xml version="1.0" encoding="UTF-8"?>
<page xmlns="http://www.xwiki.org">
  <title>Prueba de objetos V&amp;V</title>
  <content>Pagina creada por las pruebas E2E de la funcionalidad objects.</content>
</page>`
    }).then((res) => {
      expect(res.status).to.be.oneOf([201, 202]);
    });

    cy.request({
      method: 'POST',
      url: urlObjetos,
      auth,
      headers: { 'Content-Type': 'application/xml', Accept: 'application/json' },
      body: xmlObjeto('vyv-objects')
    }).then((res) => {
      expect(res.status).to.eq(201);
      numeroObjeto = res.body.number;
      expect(numeroObjeto).to.be.a('number');
    });
  });

  after(() => {
    // Limpieza: borrar la pagina de trabajo completa (idempotente).
    cy.request({ method: 'DELETE', url: urlPagina, auth, failOnStatusCode: false });
  });

  it('GET devuelve el objeto existente con su clase y numero (camino exito)', () => {
    cy.request({
      url: `${urlObjetos}/${clase}/${numeroObjeto}`,
      auth,
      headers: { Accept: 'application/json' }
    }).then((res) => {
      expect(res.status).to.eq(200);
      expect(res.body.className).to.eq(clase);
      expect(res.body.number).to.eq(numeroObjeto);
      const tags = (res.body.properties || []).find((p) => p.name === 'tags');
      expect(tags, 'la propiedad tags del objeto').to.exist;
      expect(tags.value).to.eq('vyv-objects');
    });
  });

  it('GET de un numero inexistente responde 404 (camino objeto no encontrado)', () => {
    cy.request({
      url: `${urlObjetos}/${clase}/9999`,
      auth,
      failOnStatusCode: false
    }).then((res) => {
      expect(res.status).to.eq(404);
    });
  });

  it('PUT actualiza el objeto y responde 202 ACCEPTED (camino actualizacion)', () => {
    cy.request({
      method: 'PUT',
      url: `${urlObjetos}/${clase}/${numeroObjeto}`,
      auth,
      headers: { 'Content-Type': 'application/xml', Accept: 'application/json' },
      body: xmlObjeto('vyv-objects-actualizado')
    }).then((res) => {
      expect(res.status).to.eq(202);
      const tags = (res.body.properties || []).find((p) => p.name === 'tags');
      expect(tags.value).to.eq('vyv-objects-actualizado');
    });
  });

  it('PUT sin credenciales no modifica el objeto (camino sin permiso)', () => {
    cy.request({
      method: 'PUT',
      url: `${urlObjetos}/${clase}/${numeroObjeto}`,
      headers: { 'Content-Type': 'application/xml' },
      failOnStatusCode: false,
      body: xmlObjeto('intruso')
    }).then((res) => {
      expect(res.status).to.be.oneOf([401, 403]);
      // Y el valor sigue intacto:
      cy.request({
        url: `${urlObjetos}/${clase}/${numeroObjeto}`,
        auth,
        headers: { Accept: 'application/json' }
      }).then((verif) => {
        const tags = (verif.body.properties || []).find((p) => p.name === 'tags');
        expect(tags.value).to.eq('vyv-objects-actualizado');
      });
    });
  });

  it('DELETE sin credenciales responde 401/403 y el objeto sobrevive', () => {
    cy.request({
      method: 'DELETE',
      url: `${urlObjetos}/${clase}/${numeroObjeto}`,
      failOnStatusCode: false
    }).then((res) => {
      expect(res.status).to.be.oneOf([401, 403]);
      cy.request({
        url: `${urlObjetos}/${clase}/${numeroObjeto}`,
        auth,
        failOnStatusCode: false
      }).then((verif) => expect(verif.status).to.eq(200));
    });
  });

  it('DELETE elimina el objeto y el GET posterior responde 404 (camino borrado)', () => {
    cy.request({
      method: 'DELETE',
      url: `${urlObjetos}/${clase}/${numeroObjeto}`,
      auth
    }).then((res) => {
      expect(res.status).to.be.oneOf([204, 200]);
      cy.request({
        url: `${urlObjetos}/${clase}/${numeroObjeto}`,
        auth,
        failOnStatusCode: false
      }).then((verif) => expect(verif.status).to.eq(404));
    });
  });

  it('DELETE de un objeto inexistente responde 404', () => {
    cy.request({
      method: 'DELETE',
      url: `${urlObjetos}/${clase}/9999`,
      auth,
      failOnStatusCode: false
    }).then((res) => {
      expect(res.status).to.eq(404);
    });
  });
});
