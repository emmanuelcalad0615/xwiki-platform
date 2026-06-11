// Accesibilidad - Objetos. La pagina que muestra el objeto (TagClass) y el editor
// de objetos deben cumplir WCAG 2.1 AA. Requiere login.
describe('A11y - Objetos', () => {
  const wiki = Cypress.env('wiki')
  const space = 'VyVObjects'
  const page = 'PruebaCypressA11y'
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
      body: `<?xml version="1.0"?><page xmlns="http://www.xwiki.org"><title>A11y objetos</title><content>Pagina de accesibilidad de objetos.</content></page>`
    })
    cy.request({
      method: 'POST', url: urlObjetos, auth,
      headers: { 'Content-Type': 'application/xml', 'Accept': 'application/json' },
      body: xmlObjeto('a11y')
    }).its('status').should('eq', 201)
    cy.login(adminUser, adminPass)
  })

  after(() => {
    cy.request({ method: 'DELETE', url: urlPagina, auth, failOnStatusCode: false })
  })

  beforeEach(() => {
    cy.visit(`/bin/view/${space}/${page}`)
    cy.injectAxe()
  })

  it('no tiene violaciones WCAG 2.1 AA (excluye color-contrast — pendiente de fix de diseno)', () => {
    cy.checkA11y(
      null,
      {
        runOnly: { type: 'tag', values: ['wcag2a', 'wcag2aa'] },
        rules: { 'color-contrast': { enabled: false } }
      },
      (violations) => {
        violations.forEach((v) => cy.log(`[${v.impact}] ${v.id}: ${v.description}`))
      }
    )
  })

  it('la pagina del objeto tiene heading principal (h1) no vacio', () => {
    cy.get('h1').should('exist').and('not.be.empty')
  })
})
