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

  it('Should be able to delete a tag via the Web UI', () => {
    // 1. Añadir etiqueta
    const tagToDelete = `delete-me-${Date.now()}`
    
    cy.get('body').then($body => {
      if ($body.find('.tag-add a, a#addtag, a.action-tag').length > 0) {
        cy.get('.tag-add a, a#addtag, a.action-tag').first().click({ force: true })
      }
    })

    cy.get('.tag-add-form input[type="text"], input[name="tag"]', { timeout: 10000 })
      .should('exist')
      .type(`${tagToDelete}{enter}`, { force: true })

    cy.get('body').then($body => {
      if ($body.find('.tag-save, button#save-tags, button[title="Save"]').length > 0) {
        cy.get('.tag-save, button#save-tags, button[title="Save"]').first().click({ force: true })
      }
    })
    cy.reload()

    // 2. Localizar y clickear la "x" de borrar para esa etiqueta particular
    // XWiki suele tener un <span> o <a> con clase .tag-delete al lado del tag
    cy.contains(tagToDelete).closest('.tag-wrapper').within(() => {
      cy.get('a.tag-delete').click({ force: true })
    })

    // 3. Recargar y comprobar que ya no existe en la UI
    cy.reload()
    cy.get('body').should('not.contain', tagToDelete)
  })

  it('Should render XSS payloads securely in the UI without execution', () => {
    const xssPayload = `<img src=x onerror=alert('xss-ui-${Date.now()}')>`
    
    // Lo añadimos por API de forma rápida
    const wiki = Cypress.env('wiki')
    const tagsEndpoint = `/rest/wikis/${wiki}/spaces/${space}/pages/${page}/tags`
    
    cy.request({
      method: 'PUT',
      url: tagsEndpoint,
      auth: { user: adminUser, pass: adminPass },
      headers: { 'Accept': 'application/json', 'Content-Type': 'application/json' },
      body: { tags: [{ name: xssPayload }] }
    })

    // Visitamos la UI y buscamos que el texto exista en el DOM como texto puro, no como elemento <img onerror>
    cy.reload()
    cy.get('#xdocTags, #document-tags, .page-tags, .tag-list').should('contain.text', xssPayload)
    // Verificamos explícitamente que no exista la imagen inyectada en el DOM
    cy.get('img[onerror]').should('not.exist')
  })

  it('Should handle adding multiple tags in a row persisting correctly', () => {
    const tagA = `multi-A-${Date.now()}`
    const tagB = `multi-B-${Date.now()}`

    cy.get('body').then($body => {
      if ($body.find('.tag-add a, a#addtag, a.action-tag').length > 0) {
        cy.get('.tag-add a, a#addtag, a.action-tag').first().click({ force: true })
      }
    })

    // En XWiki, puedes agregar múltiples separadas por coma, o una tras otra pulsando enter
    cy.get('.tag-add-form input[type="text"], input[name="tag"]')
      .should('exist')
      .type(`${tagA}, ${tagB}{enter}`, { force: true })

    cy.get('body').then($body => {
      if ($body.find('.tag-save, button#save-tags, button[title="Save"]').length > 0) {
        cy.get('.tag-save, button#save-tags, button[title="Save"]').first().click({ force: true })
      }
    })

    cy.reload()
    cy.get('#xdocTags, #document-tags, .page-tags, .tag-list').should('contain.text', tagA)
    cy.get('#xdocTags, #document-tags, .page-tags, .tag-list').should('contain.text', tagB)
  })
})
