// API — Comentarios. Espeja las pruebas unitarias de CommentsResourceImpl.
describe('API - Comentarios', () => {
  const wiki = Cypress.env('wiki')
  const space = Cypress.env('space')
  const page = Cypress.env('page')
  const adminUser = Cypress.env('adminUser')
  const adminPass = Cypress.env('adminPass')

  const commentsEndpoint = `/rest/wikis/${wiki}/spaces/${space}/pages/${page}/comments`

  it('Obtiene los comentarios de una pagina (GET 200)', () => {
    cy.request({
      method: 'GET',
      url: commentsEndpoint,
      headers: { 'Accept': 'application/json' }
    }).then((response) => {
      expect(response.status).to.eq(200)
      expect(response.body).to.have.property('comments')
    })
  })

  it('Crea un comentario (POST 201)', () => {
    const text = `cypress-comment-${Date.now()}`

    cy.request({
      method: 'POST',
      url: commentsEndpoint,
      auth: { user: adminUser, pass: adminPass },
      headers: { 'Accept': 'application/json', 'Content-Type': 'application/json' },
      body: { text }
    }).then((response) => {
      expect(response.status).to.eq(201)
    })

    cy.request({
      method: 'GET',
      url: commentsEndpoint,
      headers: { 'Accept': 'application/json' }
    }).then((response) => {
      expect(JSON.stringify(response.body)).to.include(text)
    })
  })

  it('Obtiene un comentario por id (GET 200)', () => {
    // Garantizar que existe al menos un comentario y leer su id
    cy.request({
      method: 'POST', url: commentsEndpoint,
      auth: { user: adminUser, pass: adminPass },
      headers: { 'Accept': 'application/json', 'Content-Type': 'application/json' },
      body: { text: `byid-${Date.now()}` }
    }).its('status').should('eq', 201)

    cy.request({ method: 'GET', url: commentsEndpoint, headers: { 'Accept': 'application/json' } })
      .then((list) => {
        const id = list.body.comments[0].id
        cy.request({ method: 'GET', url: `${commentsEndpoint}/${id}`, headers: { 'Accept': 'application/json' } })
          .then((response) => {
            // Espeja getComment_WhenIdExistsFirst_ShouldReturnComment
            expect(response.status).to.eq(200)
          })
      })
  })

  it('Crea un comentario con highlight (POST 201)', () => {
    // Espeja postComment_WhenOnlyHighlight / postComment_WhenHighlightTextAndReplyTo
    cy.request({
      method: 'POST', url: commentsEndpoint,
      auth: { user: adminUser, pass: adminPass },
      headers: { 'Accept': 'application/json', 'Content-Type': 'application/json' },
      body: { highlight: 'fragmento-resaltado', text: `hl-${Date.now()}` }
    }).its('status').should('eq', 201)
  })
})
