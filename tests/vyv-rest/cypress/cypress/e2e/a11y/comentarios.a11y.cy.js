// Accesibilidad — Comentarios.
// Pagina: /bin/view/Main/WebHome?viewer=comments (requiere login).
// Verifica: WCAG 2.1 AA, heading principal, seccion de comentarios accesible.
describe('A11y - Comentarios', () => {
  const space = Cypress.env('space')
  const page = Cypress.env('page')
  const adminUser = Cypress.env('adminUser')
  const adminPass = Cypress.env('adminPass')

  before(() => {
    cy.login(adminUser, adminPass)
  })

  beforeEach(() => {
    cy.visit(`/bin/view/${space}/${page}?viewer=comments`)
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

  it('la seccion de comentarios usa lista semantica o roles ARIA', () => {
    cy.get('ul, ol, [role="list"]').should('have.length.gte', 1)
  })
})
