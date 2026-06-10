describe('Page Tags Security Tests', () => {
  const wiki = Cypress.env('wiki')
  const space = Cypress.env('space')
  const page = Cypress.env('page')
  const tagsEndpoint = `/rest/wikis/${wiki}/spaces/${space}/pages/${page}/tags`

  it('Should return 401 Unauthorized when trying to PUT tags without authentication', () => {
    cy.request({
      method: 'PUT',
      url: tagsEndpoint,
      failOnStatusCode: false,
      headers: {
        'Accept': 'application/json',
        'Content-Type': 'application/json'
      },
      body: {
        tags: [{ name: 'hacker-tag' }]
      }
    }).then((response) => {
      // Sin autenticación debe rechazar la escritura
      expect(response.status).to.eq(401)
    })
  })

  it('Should return 401/403 when trying to PUT tags with invalid/guest credentials', () => {
    cy.request({
      method: 'PUT',
      url: tagsEndpoint,
      failOnStatusCode: false,
      auth: { user: 'InvalidUser', pass: 'wrongpass' },
      headers: { 'Accept': 'application/json', 'Content-Type': 'application/json' },
      body: {
        tags: [{ name: 'hacker-tag' }]
      }
    }).then((response) => {
      // Credenciales inválidas o sin permisos de edición
      expect(response.status).to.be.oneOf([401, 403])
    })
  })
})
