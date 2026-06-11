// UI Web — Comentarios. El comentario se crea por API y se verifica en la interfaz
// (lado lectura), incluyendo verificacion de seguridad XSS en el render.
describe('UI - Comentarios', () => {
  const wiki = Cypress.env('wiki')
  const space = Cypress.env('space')
  const page = Cypress.env('page')
  const adminUser = Cypress.env('adminUser')
  const adminPass = Cypress.env('adminPass')

  const commentsEndpoint = `/rest/wikis/${wiki}/spaces/${space}/pages/${page}/comments`
  const auth = { user: adminUser, pass: adminPass }
  const json = { 'Accept': 'application/json', 'Content-Type': 'application/json' }

  beforeEach(() => {
    cy.request({
      method: 'POST',
      url: '/bin/loginsubmit/XWiki/XWikiLogin',
      form: true,
      body: { j_username: adminUser, j_password: adminPass, submit: '1' }
    })
  })

  it('Muestra en la UI un comentario creado', () => {
    const text = `ui-comment-${Date.now()}`

    cy.request({ method: 'POST', url: commentsEndpoint, auth, headers: json, body: { text } })
      .its('status').should('eq', 201)

    cy.visit(`/bin/view/${space}/${page}?viewer=comments`)
    cy.get('body').should('contain.text', text)
  })

  it('Renderiza de forma segura un comentario con XSS (escapado, no ejecutado)', () => {
    const marker = `xss-ui-${Date.now()}`
    const xss = `<script>alert('${marker}')</script>`

    cy.request({ method: 'POST', url: commentsEndpoint, auth, headers: json, body: { text: xss } })
      .its('status').should('eq', 201)

    cy.visit(`/bin/view/${space}/${page}?viewer=comments`)

    cy.get('body').should('contain.text', marker)
    cy.get('script').then(($scripts) => {
      const injected = [...$scripts].some(s => (s.textContent || '').includes(marker))
      expect(injected).to.eq(false)
    })
  })
})
