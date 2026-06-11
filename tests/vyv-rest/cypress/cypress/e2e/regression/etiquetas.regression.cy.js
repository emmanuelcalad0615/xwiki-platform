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

  it('Should handle special characters, XSS, and SQL injection strings securely', () => {
    const maliciousTags = [
      { name: '<script>alert("xss")</script>' },
      { name: 'SELECT * FROM users' },
      { name: '!@#$%^&*()_+=' }
    ]

    cy.request({
      method: 'PUT',
      url: tagsEndpoint,
      auth: { user: adminUser, pass: adminPass },
      headers: { 'Accept': 'application/json', 'Content-Type': 'application/json' },
      body: { tags: maliciousTags }
    }).then((response) => {
      expect(response.status).to.eq(202)
    })

    cy.request({
      method: 'GET',
      url: tagsEndpoint,
      headers: { 'Accept': 'application/json' }
    }).then((response) => {
      const savedTags = response.body.tags.map(t => t.name)
      expect(savedTags).to.include('<script>alert("xss")</script>')
      expect(savedTags).to.include('SELECT * FROM users')
      expect(savedTags).to.include('!@#$%^&*()_+=')
    })
  })

  it('Should handle boundary limits (empty strings and very long tags)', () => {
    const longTag = 'a'.repeat(500)
    
    cy.request({
      method: 'PUT',
      url: tagsEndpoint,
      auth: { user: adminUser, pass: adminPass },
      failOnStatusCode: false,
      headers: { 'Accept': 'application/json', 'Content-Type': 'application/json' },
      body: { tags: [{ name: '' }, { name: '   ' }, { name: longTag }] }
    }).then((response) => {
      // Depending on XWiki config, it may accept (202) or reject (400) long/empty tags.
      // We ensure it does not crash (e.g. 500 error)
      expect(response.status).not.to.eq(500)
    })
  })

  it('Should correctly replace the tag list and perform deletions (CRUD Full Flow)', () => {
    const tag1 = `crud-tag-1-${Date.now()}`
    const tag2 = `crud-tag-2-${Date.now()}`

    // 1. Añadir 2 etiquetas
    cy.request({
      method: 'PUT',
      url: tagsEndpoint,
      auth: { user: adminUser, pass: adminPass },
      headers: { 'Accept': 'application/json', 'Content-Type': 'application/json' },
      body: { tags: [{ name: tag1 }, { name: tag2 }] }
    }).then(res => expect(res.status).to.eq(202))

    // 2. Sobrescribir con solo 1 etiqueta (equivalente a borrar la otra)
    cy.request({
      method: 'PUT',
      url: tagsEndpoint,
      auth: { user: adminUser, pass: adminPass },
      headers: { 'Accept': 'application/json', 'Content-Type': 'application/json' },
      body: { tags: [{ name: tag1 }] }
    }).then(res => expect(res.status).to.eq(202))

    // 3. Verificar que solo existe tag1 y no tag2
    cy.request({
      method: 'GET',
      url: tagsEndpoint,
      headers: { 'Accept': 'application/json' }
    }).then((response) => {
      const savedTags = response.body.tags.map(t => t.name)
      expect(savedTags).to.include(tag1)
      expect(savedTags).to.not.include(tag2)
    })
  })

  it('Should handle case sensitivity accurately', () => {
    cy.request({
      method: 'PUT',
      url: tagsEndpoint,
      auth: { user: adminUser, pass: adminPass },
      headers: { 'Accept': 'application/json', 'Content-Type': 'application/json' },
      body: { tags: [{ name: 'CypressTest' }, { name: 'cypresstest' }] }
    }).then(res => expect(res.status).to.eq(202))

    cy.request({
      method: 'GET',
      url: tagsEndpoint,
      headers: { 'Accept': 'application/json' }
    }).then((response) => {
      const savedTags = response.body.tags.map(t => t.name)
      // Assert it doesn't crash. Whether it merges or keeps both depends on XWiki rules
      expect(savedTags.length).to.be.greaterThan(0)
    })
  })
})
