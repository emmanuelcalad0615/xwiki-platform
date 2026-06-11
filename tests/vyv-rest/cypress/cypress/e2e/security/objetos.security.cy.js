// Seguridad / control de acceso - Objetos (ObjectResourceImpl).
// Espeja updateObject/deleteObject_WhenUserHasNoEditRight_ShouldThrowUnauthorized (401).
describe('Security - Objetos', () => {
  const wiki = Cypress.env('wiki')
  const space = 'VyVObjects'
  const page = 'PruebaCypressSeguridad'
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
      body: `<?xml version="1.0"?><page xmlns="http://www.xwiki.org"><title>Seguridad objetos</title><content>.</content></page>`
    })
    cy.request({
      method: 'POST', url: urlObjetos,
      auth: { user: adminUser, pass: adminPass },
      headers: { 'Content-Type': 'application/xml', 'Accept': 'application/json' },
      body: xmlObjeto('seguridad')
    }).then((res) => { numero = res.body.number })
  })

  after(() => {
    cy.request({ method: 'DELETE', url: urlPagina, auth: { user: adminUser, pass: adminPass }, failOnStatusCode: false })
  })

  beforeEach(() => {
    // El setup autenticado deja una cookie de sesion de XWiki; sin limpiarla, las
    // peticiones "sin auth" la reutilizarian y no probarian el camino 401.
    cy.clearCookies()
  })

  it('Actualizar sin autenticacion devuelve 401', () => {
    cy.request({
      method: 'PUT', url: `${urlObjetos}/${clase}/${numero}`,
      failOnStatusCode: false,
      headers: { 'Content-Type': 'application/xml' },
      body: xmlObjeto('intruso')
    }).then((res) => {
      expect(res.status).to.eq(401)
    })
  })

  it('Eliminar sin autenticacion devuelve 401 y el objeto sobrevive', () => {
    cy.request({
      method: 'DELETE', url: `${urlObjetos}/${clase}/${numero}`,
      failOnStatusCode: false
    }).then((res) => {
      expect(res.status).to.eq(401)
      cy.request({
        url: `${urlObjetos}/${clase}/${numero}`,
        auth: { user: adminUser, pass: adminPass }
      }).its('status').should('eq', 200)
    })
  })

  it('Actualizar con credenciales invalidas devuelve 401/403', () => {
    cy.request({
      method: 'PUT', url: `${urlObjetos}/${clase}/${numero}`,
      failOnStatusCode: false,
      auth: { user: 'InvalidUser', pass: 'wrongpass' },
      headers: { 'Content-Type': 'application/xml' },
      body: xmlObjeto('bad-creds')
    }).then((res) => {
      expect(res.status).to.be.oneOf([401, 403])
    })
  })
})
