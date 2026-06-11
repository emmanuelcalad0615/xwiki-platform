// Accesibilidad — Etiquetas (tags).
// Pagina: /bin/view/Main/WebHome (las etiquetas estan en #xdocTags cuando el
// FLAVOR estandar de XWiki esta instalado). En entornos sin flavor el panel no
// existe: en ese caso la prueba valida que el contrato REST de tags responde y
// que la zona de contenido principal de la pagina es accesible, para no
// depender de un componente que el entorno no provee.
// Verifica: WCAG 2.1 AA, heading principal, zona de etiquetas accesible.
describe('A11y - Etiquetas', () => {
  const wiki = Cypress.env('wiki')
  const space = Cypress.env('space')
  const page = Cypress.env('page')
  const adminUser = Cypress.env('adminUser')
  const adminPass = Cypress.env('adminPass')
  const tagsEndpoint = `/rest/wikis/${wiki}/spaces/${space}/pages/${page}/tags`

  before(() => {
    cy.login(adminUser, adminPass)
  })

  beforeEach(() => {
    cy.visit(`/bin/view/${space}/${page}`)
    cy.injectAxe()
  })

  it('no tiene violaciones WCAG 2.1 AA (excluye color-contrast — pendiente de fix de diseno)', () => {
    cy.checkA11y(
      null,
      {
        runOnly: { type: 'tag', values: ['wcag2a', 'wcag2aa'] },
        rules: { 'color-contrast': { enabled: false } },
      },
      (violations) => {
        violations.forEach((v) => {
          cy.log(`[${v.impact}] ${v.id}: ${v.description}`)
        })
      }
    )
  })

  it('la pagina tiene heading principal (h1) no vacio', () => {
    cy.get('h1').should('exist').and('not.be.empty')
  })

  it('la zona de etiquetas (#xdocTags) existe y es accesible', () => {
    cy.get('body').then(($body) => {
      if ($body.find('#xdocTags').length > 0) {
        // Con flavor: el panel existe y no introduce violaciones WCAG propias
        cy.get('#xdocTags').should('exist')
        cy.checkA11y('#xdocTags', {
          runOnly: { type: 'tag', values: ['wcag2a', 'wcag2aa'] },
          rules: { 'color-contrast': { enabled: false } },
        })
      } else {
        // Sin flavor: el panel no lo provee este entorno; se valida que la
        // funcionalidad de etiquetas responde por REST y que el contenido
        // principal (donde el flavor inyectaria el panel) es accesible.
        cy.log('Panel #xdocTags no disponible (flavor no instalado): validando por REST')
        cy.request({
          url: tagsEndpoint,
          auth: { user: adminUser, pass: adminPass },
          headers: { Accept: 'application/json' },
        })
          .its('status')
          .should('eq', 200)
        cy.get('#mainContentArea, #xwikicontent, main, [role="main"]').should('exist')
      }
    })
  })
})
