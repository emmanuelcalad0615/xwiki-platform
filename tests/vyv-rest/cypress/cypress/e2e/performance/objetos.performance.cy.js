// Performance - Objetos (ObjectResourceImpl). Mide res.duration (ms) de la API
// REST de objetos contra un umbral.
describe('Performance - Objetos', () => {
  const wiki = Cypress.env('wiki')
  const space = 'VyVObjects'
  const page = 'PruebaCypressPerf'
  const clase = 'XWiki.TagClass'
  const adminUser = Cypress.env('adminUser')
  const adminPass = Cypress.env('adminPass')

  const urlPagina = `/rest/wikis/${wiki}/spaces/${space}/pages/${page}`
  const urlObjetos = `${urlPagina}/objects`
  const UMBRAL_MS = 2000

  const xmlObjeto = (tags) =>
    `<?xml version="1.0" encoding="UTF-8"?>` +
    `<object xmlns="http://www.xwiki.org"><className>${clase}</className>` +
    `<property name="tags"><value>${tags}</value></property></object>`

  let numero

  before(() => {
    cy.request({
      method: 'PUT', url: urlPagina,
      auth: { user: adminUser, pass: adminPass },
      headers: { 'Content-Type': 'application/xml' },
      body: `<?xml version="1.0"?><page xmlns="http://www.xwiki.org"><title>Perf objetos</title><content>.</content></page>`
    })
    cy.request({
      method: 'POST', url: urlObjetos,
      auth: { user: adminUser, pass: adminPass },
      headers: { 'Content-Type': 'application/xml', 'Accept': 'application/json' },
      body: xmlObjeto('perf')
    }).then((res) => { numero = res.body.number })
  })

  after(() => {
    cy.request({ method: 'DELETE', url: urlPagina, auth: { user: adminUser, pass: adminPass }, failOnStatusCode: false })
  })

  it(`GET de un objeto responde en menos de ${UMBRAL_MS} ms`, () => {
    cy.request({
      url: `${urlObjetos}/${clase}/${numero}`,
      headers: { 'Accept': 'application/json' },
      auth: { user: adminUser, pass: adminPass }
    }).then((res) => {
      expect(res.status).to.eq(200)
      cy.log(`GET objeto: ${res.duration} ms`)
      expect(res.duration).to.be.lessThan(UMBRAL_MS)
    })
  })

  it(`GET de la lista de objetos responde en menos de ${UMBRAL_MS} ms`, () => {
    cy.request({
      url: urlObjetos,
      headers: { 'Accept': 'application/json' },
      auth: { user: adminUser, pass: adminPass }
    }).then((res) => {
      expect(res.status).to.eq(200)
      cy.log(`GET lista: ${res.duration} ms`)
      expect(res.duration).to.be.lessThan(UMBRAL_MS)
    })
  })

  it(`Promedio de 10 GET de objeto bajo el umbral (${UMBRAL_MS} ms)`, () => {
    const tiempos = []
    Cypress._.times(10, () => {
      cy.request({
        url: `${urlObjetos}/${clase}/${numero}`,
        headers: { 'Accept': 'application/json' },
        auth: { user: adminUser, pass: adminPass }
      }).then((res) => tiempos.push(res.duration))
    })
    cy.then(() => {
      const prom = tiempos.reduce((a, b) => a + b, 0) / tiempos.length
      cy.log(`Promedio 10 GET: ${prom.toFixed(0)} ms`)
      expect(prom).to.be.lessThan(UMBRAL_MS)
    })
  })
})
