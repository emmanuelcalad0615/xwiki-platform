// Regresion - Objetos (ObjectResourceImpl). Recorre el ciclo de vida completo
// (crear -> leer -> actualizar -> borrar -> 404) para detectar rupturas del
// contrato REST entre versiones.
describe('Regression - Objetos', () => {
  const wiki = Cypress.env('wiki')
  const space = 'VyVObjects'
  const page = 'PruebaCypressRegresion'
  const clase = 'XWiki.TagClass'
  const adminUser = Cypress.env('adminUser')
  const adminPass = Cypress.env('adminPass')

  const urlPagina = `/rest/wikis/${wiki}/spaces/${space}/pages/${page}`
  const urlObjetos = `${urlPagina}/objects`
  const auth = { user: adminUser, pass: adminPass }

  const xmlObjeto = (tags) =>
    `<?xml version="1.0" encoding="UTF-8"?>` +
    `<object xmlns="http://www.xwiki.org"><className>${clase}</className>` +
    `<property name="tags"><value>${tags}</value></property></object>`

  before(() => {
    cy.request({
      method: 'PUT', url: urlPagina, auth,
      headers: { 'Content-Type': 'application/xml' },
      body: `<?xml version="1.0"?><page xmlns="http://www.xwiki.org"><title>Regresion objetos</title><content>.</content></page>`
    })
  })

  after(() => {
    cy.request({ method: 'DELETE', url: urlPagina, auth, failOnStatusCode: false })
  })

  it('Ciclo de vida completo del objeto mantiene el contrato REST', () => {
    // Crear (201)
    cy.request({
      method: 'POST', url: urlObjetos, auth,
      headers: { 'Content-Type': 'application/xml', 'Accept': 'application/json' },
      body: xmlObjeto('v1')
    }).then((creado) => {
      expect(creado.status).to.eq(201)
      const n = creado.body.number

      // Leer (200) con el valor inicial
      cy.request({ url: `${urlObjetos}/${clase}/${n}`, headers: { 'Accept': 'application/json' }, auth })
        .then((leido) => {
          expect(leido.status).to.eq(200)
          const tags = leido.body.properties.find((p) => p.name === 'tags')
          expect(tags.value).to.eq('v1')
        })

      // Actualizar (202) y verificar el nuevo valor
      cy.request({
        method: 'PUT', url: `${urlObjetos}/${clase}/${n}`, auth,
        headers: { 'Content-Type': 'application/xml', 'Accept': 'application/json' },
        body: xmlObjeto('v2')
      }).its('status').should('eq', 202)

      cy.request({ url: `${urlObjetos}/${clase}/${n}`, headers: { 'Accept': 'application/json' }, auth })
        .then((reLeido) => {
          const tags = reLeido.body.properties.find((p) => p.name === 'tags')
          expect(tags.value).to.eq('v2')
        })

      // Borrar y confirmar 404 posterior
      cy.request({ method: 'DELETE', url: `${urlObjetos}/${clase}/${n}`, auth })
        .its('status').should('be.oneOf', [200, 204])
      cy.request({ url: `${urlObjetos}/${clase}/${n}`, auth, failOnStatusCode: false })
        .its('status').should('eq', 404)
    })
  })

  it('El numero de objeto se mantiene estable entre lecturas', () => {
    cy.request({
      method: 'POST', url: urlObjetos, auth,
      headers: { 'Content-Type': 'application/xml', 'Accept': 'application/json' },
      body: xmlObjeto('estable')
    }).then((creado) => {
      const n = creado.body.number
      cy.request({ url: `${urlObjetos}/${clase}/${n}`, headers: { 'Accept': 'application/json' }, auth })
        .its('body.number').should('eq', n)
    })
  })
})
