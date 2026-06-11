// UI Web - Objetos. El objeto se crea por API y se verifica en la interfaz:
// la etiqueta del objeto TagClass se refleja en la pagina renderizada y el
// objeto aparece en el editor de objetos (la vista que administra ObjectResourceImpl).
describe('UI - Objetos', () => {
  const wiki = Cypress.env('wiki')
  const space = 'VyVObjects'
  const page = 'PruebaCypressUI'
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
      body: `<?xml version="1.0"?><page xmlns="http://www.xwiki.org"><title>UI objetos</title><content>Pagina UI de objetos.</content></page>`
    })
    cy.request({
      method: 'POST', url: urlObjetos, auth,
      headers: { 'Content-Type': 'application/xml', 'Accept': 'application/json' },
      body: xmlObjeto('etiqueta-ui')
    }).its('status').should('eq', 201)
  })

  after(() => {
    cy.request({ method: 'DELETE', url: urlPagina, auth, failOnStatusCode: false })
  })

  beforeEach(() => {
    cy.request({
      method: 'POST',
      url: '/bin/loginsubmit/XWiki/XWikiLogin',
      form: true,
      body: { j_username: adminUser, j_password: adminPass, submit: '1' }
    })
  })

  it('La etiqueta del objeto se refleja en la pagina renderizada', () => {
    cy.visit(`/bin/view/${space}/${page}`)
    // XWiki expone el tag del objeto como keyword del documento
    cy.get('meta[name="keywords"]').should('have.attr', 'content').and('include', 'etiqueta-ui')
  })

  it('El objeto XWiki.TagClass aparece en el editor de objetos', () => {
    cy.visit(`/bin/edit/${space}/${page}?editor=object`, { failOnStatusCode: false })
    cy.get('body').should('contain.text', 'TagClass')
  })
})
