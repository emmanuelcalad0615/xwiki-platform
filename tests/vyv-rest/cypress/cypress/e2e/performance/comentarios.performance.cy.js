// Performance — Comentarios. Mide el tiempo de respuesta de la API REST de
// comentarios usando res.duration (ms) y lo compara contra un umbral.
describe('Performance - Comentarios', () => {
  const wiki = Cypress.env('wiki')
  const space = Cypress.env('space')
  const page = Cypress.env('page')
  const adminUser = Cypress.env('adminUser')
  const adminPass = Cypress.env('adminPass')

  const commentsEndpoint = `/rest/wikis/${wiki}/spaces/${space}/pages/${page}/comments`
  const UMBRAL_MS = 2000

  it(`GET de comentarios responde en menos de ${UMBRAL_MS} ms`, () => {
    cy.request({ method: 'GET', url: commentsEndpoint, headers: { 'Accept': 'application/json' } })
      .then((res) => {
        expect(res.status).to.eq(200)
        cy.log(`GET comentarios: ${res.duration} ms`)
        expect(res.duration).to.be.lessThan(UMBRAL_MS)
      })
  })

  it(`POST de un comentario responde en menos de ${UMBRAL_MS} ms`, () => {
    cy.request({
      method: 'POST', url: commentsEndpoint,
      auth: { user: adminUser, pass: adminPass },
      headers: { 'Accept': 'application/json', 'Content-Type': 'application/json' },
      body: { text: `perf-${Date.now()}` }
    }).then((res) => {
      expect(res.status).to.eq(201)
      cy.log(`POST comentario: ${res.duration} ms`)
      expect(res.duration).to.be.lessThan(UMBRAL_MS)
    })
  })

  it(`Promedio de 10 GET consecutivos bajo el umbral (${UMBRAL_MS} ms)`, () => {
    const tiempos = []
    Cypress._.times(10, () => {
      cy.request({ method: 'GET', url: commentsEndpoint, headers: { 'Accept': 'application/json' } })
        .then((res) => tiempos.push(res.duration))
    })
    cy.then(() => {
      const prom = tiempos.reduce((a, b) => a + b, 0) / tiempos.length
      cy.log(`Promedio 10 GET: ${prom.toFixed(0)} ms`)
      expect(prom).to.be.lessThan(UMBRAL_MS)
    })
  })
})
