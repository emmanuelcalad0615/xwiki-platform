describe('Page Tags UI Tests', () => {
  const space = Cypress.env('space')
  const page = Cypress.env('page')
  const adminUser = Cypress.env('adminUser')
  const adminPass = Cypress.env('adminPass')

  beforeEach(() => {
    // Autenticación programática para obtener la cookie de sesión antes de visitar la UI
    cy.request({
      method: 'POST',
      url: '/bin/loginsubmit/XWiki/XWikiLogin',
      form: true,
      body: {
        j_username: adminUser,
        j_password: adminPass,
        submit: '1'
      }
    })
    cy.visit('/bin/view/Main/')
  })

  it('Should be able to view, add and display a tag via the Web UI', () => {

    // Nota: Los selectores de XWiki pueden variar según la versión y el skin (Flamingo).
    // Usualmente las etiquetas están en un div con id #xdocTags o #document-tags

    // 1. Localizar la zona de etiquetas
    cy.get('body').then($body => {
      if ($body.find('.tag-add a, a#addtag, a.action-tag').length > 0) {
        cy.get('.tag-add a, a#addtag, a.action-tag').first().click({ force: true })
      }
    })

    const uniqueTag = `ui-tag-${Date.now()}`

    // 2. Escribir en el input de etiquetas
    // XWiki inserta un form con class="tag-add-form" vía AJAX. El input se llama "tag".
    cy.get('.tag-add-form input[type="text"], input[name="tag"]', { timeout: 10000 })
      .should('exist')
      .type(`${uniqueTag}{enter}`, { force: true })

    // 3. Forzar el guardado si hay un botón explícito (A veces se guarda al hacer 'enter')
    cy.get('body').then($body => {
      if ($body.find('.tag-save, button#save-tags, button[title="Save"]').length > 0) {
        cy.get('.tag-save, button#save-tags, button[title="Save"]').first().click({ force: true })
      }
    })

    // 4. Recargar y verificar que la etiqueta se persiste y se muestra en el DOM
    cy.reload()
    cy.get('#xdocTags, #document-tags, .page-tags, .tag-list').should('contain.text', uniqueTag)
  })
})
