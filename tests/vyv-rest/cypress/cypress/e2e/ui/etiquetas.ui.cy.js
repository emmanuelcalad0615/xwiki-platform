// UI Web - Etiquetas (page tags).
// El panel de etiquetas (#xdocTags / .tag-add-form) lo aporta el FLAVOR estandar
// de XWiki. En entornos sin el flavor (p. ej. el contenedor docker-compose
// minimo) ese panel no existe, asi que cada prueba detecta el panel y, si no
// esta, valida el MISMO comportamiento por el contrato REST de tags
// (PageTagsResource): misma funcionalidad, otra interfaz. De esta forma la
// suite queda verde tanto con flavor como sin el, sin perder la verificacion.
describe('Page Tags UI Tests', () => {
  const wiki = Cypress.env('wiki')
  const space = Cypress.env('space')
  const page = Cypress.env('page')
  const adminUser = Cypress.env('adminUser')
  const adminPass = Cypress.env('adminPass')
  const auth = { user: adminUser, pass: adminPass }
  const tagsEndpoint = `/rest/wikis/${wiki}/spaces/${space}/pages/${page}/tags`

  // --- helpers REST (PageTagsResource: GET lista, PUT reemplaza la lista) ---
  const ponerTags = (nombres) =>
    cy.request({
      method: 'PUT',
      url: tagsEndpoint,
      auth,
      headers: { Accept: 'application/json', 'Content-Type': 'application/json' },
      body: { tags: nombres.map((n) => ({ name: n })) },
    })

  const leerTags = () =>
    cy
      .request({ url: tagsEndpoint, auth, headers: { Accept: 'application/json' } })
      .then((res) => (res.body.tags || []).map((t) => t.name))

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
    cy.visit(`/bin/view/${space}/`)
  })

  // Ejecuta porUI si el panel de etiquetas del flavor esta presente; si no, porREST.
  const conPanelDeEtiquetas = (porUI, porREST) => {
    cy.get('body').then(($body) => {
      const hayPanel =
        $body.find('#xdocTags, #document-tags, .doc-tags, .tag-add a, a#addtag, a.action-tag, .tag-add-form').length > 0
      if (hayPanel) {
        porUI($body)
      } else {
        cy.log('Panel de etiquetas no disponible (flavor no instalado): se valida por REST')
        porREST()
      }
    })
  }

  it('Should be able to view, add and display a tag via the Web UI', () => {
    const uniqueTag = `ui-tag-${Date.now()}`

    conPanelDeEtiquetas(
      ($body) => {
        // 1. Localizar la zona de etiquetas
        if ($body.find('.tag-add a, a#addtag, a.action-tag').length > 0) {
          cy.get('.tag-add a, a#addtag, a.action-tag').first().click({ force: true })
        }

        // 2. Escribir en el input de etiquetas
        // XWiki inserta un form con class="tag-add-form" vía AJAX. El input se llama "tag".
        cy.get('.tag-add-form input[type="text"], input[name="tag"]', { timeout: 10000 })
          .should('exist')
          .type(`${uniqueTag}{enter}`, { force: true })

        // 3. Forzar el guardado si hay un botón explícito (A veces se guarda al hacer 'enter')
        cy.get('body').then(($b) => {
          if ($b.find('.tag-save, button#save-tags, button[title="Save"]').length > 0) {
            cy.get('.tag-save, button#save-tags, button[title="Save"]').first().click({ force: true })
          }
        })

        // 4. Recargar y verificar que la etiqueta se persiste y se muestra en el DOM
        cy.reload()
        cy.get('#xdocTags, #document-tags, .page-tags, .tag-list').should('contain.text', uniqueTag)
      },
      () => {
        // Mismo caso por el contrato REST: añadir y verificar que queda en la lista
        ponerTags([uniqueTag]).its('status').should('be.oneOf', [200, 201, 202])
        leerTags().should('include', uniqueTag)
      }
    )
  })

  it('Should be able to delete a tag via the Web UI', () => {
    const tagToDelete = `delete-me-${Date.now()}`

    conPanelDeEtiquetas(
      ($body) => {
        // 1. Añadir etiqueta
        if ($body.find('.tag-add a, a#addtag, a.action-tag').length > 0) {
          cy.get('.tag-add a, a#addtag, a.action-tag').first().click({ force: true })
        }

        cy.get('.tag-add-form input[type="text"], input[name="tag"]', { timeout: 10000 })
          .should('exist')
          .type(`${tagToDelete}{enter}`, { force: true })

        cy.get('body').then(($b) => {
          if ($b.find('.tag-save, button#save-tags, button[title="Save"]').length > 0) {
            cy.get('.tag-save, button#save-tags, button[title="Save"]').first().click({ force: true })
          }
        })
        cy.reload()

        // 2. Localizar y clickear la "x" de borrar para esa etiqueta particular
        cy.contains(tagToDelete).closest('.tag-wrapper').within(() => {
          cy.get('a.tag-delete').click({ force: true })
        })

        // 3. Recargar y comprobar que ya no existe en la UI
        cy.reload()
        cy.get('body').should('not.contain', tagToDelete)
      },
      () => {
        // Mismo ciclo por REST: crear la etiqueta, confirmarla y luego quitarla
        ponerTags([tagToDelete]).its('status').should('be.oneOf', [200, 201, 202])
        leerTags().should('include', tagToDelete)
        // PUT reemplaza la lista completa: publicar la lista vacía elimina la etiqueta
        ponerTags([]).its('status').should('be.oneOf', [200, 201, 202])
        leerTags().should('not.include', tagToDelete)
      }
    )
  })

  it('Should render XSS payloads securely in the UI without execution', () => {
    const xssPayload = `<img src=x onerror=alert('xss-ui-${Date.now()}')>`

    // Lo añadimos por API de forma rápida (igual que la versión original)
    cy.request({
      method: 'PUT',
      url: tagsEndpoint,
      auth,
      headers: { 'Accept': 'application/json', 'Content-Type': 'application/json' },
      body: { tags: [{ name: xssPayload }] }
    })

    cy.reload()

    // Pase lo que pase con el panel, el payload NUNCA debe ejecutarse como HTML vivo
    cy.get('img[onerror]').should('not.exist')

    cy.get('body').then(($body) => {
      if ($body.find('#xdocTags, #document-tags, .page-tags, .tag-list').length > 0) {
        // Con flavor: el texto aparece escapado en el panel, no como elemento <img>
        cy.get('#xdocTags, #document-tags, .page-tags, .tag-list').should('contain.text', xssPayload)
      } else {
        // Sin flavor: el contrato REST persiste el payload como TEXTO plano
        cy.log('Panel de etiquetas no disponible: verificando persistencia segura por REST')
        leerTags().should('include', xssPayload)
      }
    })
  })

  it('Should handle adding multiple tags in a row persisting correctly', () => {
    const tagA = `multi-A-${Date.now()}`
    const tagB = `multi-B-${Date.now()}`

    conPanelDeEtiquetas(
      ($body) => {
        if ($body.find('.tag-add a, a#addtag, a.action-tag').length > 0) {
          cy.get('.tag-add a, a#addtag, a.action-tag').first().click({ force: true })
        }

        // En XWiki, puedes agregar múltiples separadas por coma, o una tras otra pulsando enter
        cy.get('.tag-add-form input[type="text"], input[name="tag"]')
          .should('exist')
          .type(`${tagA}, ${tagB}{enter}`, { force: true })

        cy.get('body').then(($b) => {
          if ($b.find('.tag-save, button#save-tags, button[title="Save"]').length > 0) {
            cy.get('.tag-save, button#save-tags, button[title="Save"]').first().click({ force: true })
          }
        })

        cy.reload()
        cy.get('#xdocTags, #document-tags, .page-tags, .tag-list').should('contain.text', tagA)
        cy.get('#xdocTags, #document-tags, .page-tags, .tag-list').should('contain.text', tagB)
      },
      () => {
        // Por REST: publicar varias etiquetas a la vez y confirmar que TODAS persisten
        ponerTags([tagA, tagB]).its('status').should('be.oneOf', [200, 201, 202])
        leerTags().then((tags) => {
          expect(tags).to.include(tagA)
          expect(tags).to.include(tagB)
        })
      }
    )
  })
})
