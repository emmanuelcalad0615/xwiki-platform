// Regresion / casos borde — Comentarios (API REST).
describe('Regression - Comentarios', () => {
  const wiki = Cypress.env('wiki')
  const space = Cypress.env('space')
  const page = Cypress.env('page')
  const adminUser = Cypress.env('adminUser')
  const adminPass = Cypress.env('adminPass')

  const commentsEndpoint = `/rest/wikis/${wiki}/spaces/${space}/pages/${page}/comments`
  const auth = { user: adminUser, pass: adminPass }
  const json = { 'Accept': 'application/json', 'Content-Type': 'application/json' }

  it('Devuelve 404 para un id de comentario inexistente', () => {
    cy.request({
      method: 'GET',
      url: `${commentsEndpoint}/999999`,
      failOnStatusCode: false,
      headers: { 'Accept': 'application/json' }
    }).then((response) => {
      // Espeja getComment_WhenIdNotFound_ShouldThrowNotFound
      expect(response.status).to.eq(404)
    })
  })

  it('Crea una respuesta a un comentario (replyTo)', () => {
    cy.request({
      method: 'POST', url: commentsEndpoint, auth, headers: json,
      body: { text: `parent-${Date.now()}` }
    }).its('status').should('eq', 201)

    cy.request({
      method: 'POST', url: commentsEndpoint, auth, headers: json,
      body: { text: `reply-${Date.now()}`, replyTo: 0 }
    }).its('status').should('eq', 201)
  })

  it('Almacena un comentario con XSS / caracteres especiales sin ejecutarlo', () => {
    const xss = `<script>alert('xss-${Date.now()}')</script>`

    cy.request({
      method: 'POST', url: commentsEndpoint, auth, headers: json,
      body: { text: xss }
    }).its('status').should('eq', 201)

    cy.request({
      method: 'GET', url: commentsEndpoint, headers: { 'Accept': 'application/json' }
    }).then((response) => {
      expect(JSON.stringify(response.body)).to.include('xss-')
    })
  })

  it('Acepta un comentario vacio (comportamiento desplegado actual: 204)', () => {
    // NOTA V&V: el XWiki desplegado acepta comentario vacio y devuelve 204 sin guardar.
    // Defecto corregido a 400 en CommentsResourceImpl (ver unit test
    // postComment_WhenBodyHasNoContent_ShouldThrowBadRequest).
    cy.request({
      method: 'POST', url: commentsEndpoint, auth, headers: json,
      failOnStatusCode: false,
      body: { replyTo: 0 }
    }).then((response) => {
      expect(response.status).to.eq(204)
    })
  })

  it('Soporta paginacion con start/number', () => {
    // Espeja el uso de RangeIterable en getComments
    cy.request({
      method: 'GET',
      url: `${commentsEndpoint}?start=0&number=1`,
      headers: { 'Accept': 'application/json' }
    }).then((response) => {
      expect(response.status).to.eq(200)
      expect(response.body.comments.length).to.be.at.most(1)
    })
  })

  it('El numero de comentarios aumenta tras crear uno (CRUD)', () => {
    let antes
    cy.request({ method: 'GET', url: commentsEndpoint, headers: { 'Accept': 'application/json' } })
      .then((r) => { antes = (r.body.comments || []).length })

    cy.request({ method: 'POST', url: commentsEndpoint, auth, headers: json, body: { text: `count-${Date.now()}` } })
      .its('status').should('eq', 201)

    cy.request({ method: 'GET', url: commentsEndpoint, headers: { 'Accept': 'application/json' } })
      .then((r) => { expect((r.body.comments || []).length).to.be.greaterThan(antes) })
  })
})
