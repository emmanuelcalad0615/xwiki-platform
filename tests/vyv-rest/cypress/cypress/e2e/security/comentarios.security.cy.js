// Seguridad / control de acceso — Comentarios.
// Espeja postComment_WhenUserHasNoRights_ShouldThrowUnauthorized (401).
describe('Security - Comentarios', () => {
  const wiki = Cypress.env('wiki')
  const space = Cypress.env('space')
  const page = Cypress.env('page')
  const commentsEndpoint = `/rest/wikis/${wiki}/spaces/${space}/pages/${page}/comments`

  it('Devuelve 401 al comentar sin autenticacion', () => {
    cy.request({
      method: 'POST',
      url: commentsEndpoint,
      failOnStatusCode: false,
      headers: { 'Accept': 'application/json', 'Content-Type': 'application/json' },
      body: { text: 'sin-auth' }
    }).then((response) => {
      expect(response.status).to.eq(401)
    })
  })

  it('Devuelve 401/403 al comentar con credenciales invalidas', () => {
    cy.request({
      method: 'POST',
      url: commentsEndpoint,
      failOnStatusCode: false,
      auth: { user: 'InvalidUser', pass: 'wrongpass' },
      headers: { 'Accept': 'application/json', 'Content-Type': 'application/json' },
      body: { text: 'bad-creds' }
    }).then((response) => {
      expect(response.status).to.be.oneOf([401, 403])
    })
  })
})
