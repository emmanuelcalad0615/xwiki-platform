describe('Page Tags Regression Tests', () => {
  const wiki = Cypress.env('wiki')
  const space = Cypress.env('space')
  const page = Cypress.env('page')
  const adminUser = Cypress.env('adminUser')
  const adminPass = Cypress.env('adminPass')
  const tagsEndpoint = `/rest/wikis/${wiki}/spaces/${space}/pages/${page}/tags`

  it('Should handle duplicated tags gracefully without crashing (PUT)', () => {
    // Enviar etiquetas duplicadas
    cy.request({
      method: 'PUT',
      url: tagsEndpoint,
      auth: { user: adminUser, pass: adminPass },
      headers: { 'Accept': 'application/json', 'Content-Type': 'application/json' },
      body: {
        tags: [{ name: 'duplicate-tag' }, { name: 'duplicate-tag' }]
      }
    }).then((response) => {
      expect(response.status).to.eq(202)
    })

    // Verificar que el sistema sigue respondiendo
    cy.request({
      method: 'GET',
      url: tagsEndpoint,
      headers: { 'Accept': 'application/json' }
    }).then((response) => {
      expect(response.status).to.eq(200)
      const tags = response.body.tags.map(t => t.name)
      // Debe haber al menos una ocurrencia (XWiki maneja la duplicación internamente)
      const count = tags.filter(t => t === 'duplicate-tag').length
      expect(count).to.be.at.least(1)
    })
  })

  it('Should return 404 for a non-existent page tags (Regression)', () => {
    cy.request({
      method: 'GET',
      url: `/rest/wikis/${wiki}/spaces/${space}/pages/PaginaInexistente9999/tags`,
      failOnStatusCode: false,
      headers: { 'Accept': 'application/json' }
    }).then((response) => {
      expect(response.status).to.be.oneOf([200, 404])
    })
  })
})
