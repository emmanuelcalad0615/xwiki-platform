// API - Objetos (ObjectResourceImpl). Espeja las pruebas unitarias de getObject,
// updateObject y deleteObject contra el XWiki real.
// Nota: el REST de XWiki rechaza form-urlencoded (CSRF 403); se usa XML, que es
// ademas el contrato canonico del recurso de objetos.
describe('API - Objetos', () => {
  const wiki = Cypress.env('wiki')
  const space = 'VyVObjects'
  const page = 'PruebaCypressApi'
  const clase = 'XWiki.TagClass'
  const adminUser = Cypress.env('adminUser')
  const adminPass = Cypress.env('adminPass')

  const urlPagina = `/rest/wikis/${wiki}/spaces/${space}/pages/${page}`
  const urlObjetos = `${urlPagina}/objects`

  const xmlObjeto = (tags) =>
    `<?xml version="1.0" encoding="UTF-8"?>` +
    `<object xmlns="http://www.xwiki.org"><className>${clase}</className>` +
    `<property name="tags"><value>${tags}</value></property></object>`

  let numero

  before(() => {
    cy.request({
      method: 'PUT', url: urlPagina,
      auth: { user: adminUser, pass: adminPass },
      headers: { 'Content-Type': 'application/xml' },
      body: `<?xml version="1.0"?><page xmlns="http://www.xwiki.org"><title>API objetos</title><content>.</content></page>`
    })
    cy.request({
      method: 'POST', url: urlObjetos,
      auth: { user: adminUser, pass: adminPass },
      headers: { 'Content-Type': 'application/xml', 'Accept': 'application/json' },
      body: xmlObjeto('cypress-api')
    }).then((res) => {
      expect(res.status).to.eq(201)
      numero = res.body.number
    })
  })

  after(() => {
    cy.request({ method: 'DELETE', url: urlPagina, auth: { user: adminUser, pass: adminPass }, failOnStatusCode: false })
  })

  it('Obtiene un objeto por clase y numero (GET 200)', () => {
    cy.request({
      url: `${urlObjetos}/${clase}/${numero}`,
      headers: { 'Accept': 'application/json' },
      auth: { user: adminUser, pass: adminPass }
    }).then((res) => {
      // Espeja getObject_WhenObjectExists_ShouldReturnRestObject
      expect(res.status).to.eq(200)
      expect(res.body.className).to.eq(clase)
      expect(res.body.number).to.eq(numero)
    })
  })

  it('Objeto inexistente devuelve 404 (GET)', () => {
    cy.request({
      url: `${urlObjetos}/${clase}/9999`,
      failOnStatusCode: false,
      auth: { user: adminUser, pass: adminPass }
    }).then((res) => {
      // Espeja getObject_WhenObjectDoesNotExist_ShouldThrowNotFound
      expect(res.status).to.eq(404)
    })
  })

  it('Actualiza un objeto (PUT 202)', () => {
    cy.request({
      method: 'PUT', url: `${urlObjetos}/${clase}/${numero}`,
      auth: { user: adminUser, pass: adminPass },
      headers: { 'Content-Type': 'application/xml', 'Accept': 'application/json' },
      body: xmlObjeto('cypress-actualizado')
    }).then((res) => {
      // Espeja updateObject_WhenValid_ShouldSaveAndReturnAccepted
      expect(res.status).to.eq(202)
      const tags = (res.body.properties || []).find((p) => p.name === 'tags')
      expect(tags.value).to.eq('cypress-actualizado')
    })
  })

  it('Elimina un objeto y el GET posterior da 404 (DELETE)', () => {
    cy.request({
      method: 'POST', url: urlObjetos,
      auth: { user: adminUser, pass: adminPass },
      headers: { 'Content-Type': 'application/xml', 'Accept': 'application/json' },
      body: xmlObjeto('para-borrar')
    }).then((creado) => {
      const n = creado.body.number
      cy.request({
        method: 'DELETE', url: `${urlObjetos}/${clase}/${n}`,
        auth: { user: adminUser, pass: adminPass }
      }).then((del) => {
        // Espeja deleteObject_WhenValid_ShouldRemoveObjectAndSave
        expect(del.status).to.be.oneOf([204, 200])
        cy.request({
          url: `${urlObjetos}/${clase}/${n}`,
          failOnStatusCode: false,
          auth: { user: adminUser, pass: adminPass }
        }).then((verif) => expect(verif.status).to.eq(404))
      })
    })
  })
})
