describe('Page Tags API Tests', () => {
  const wiki = Cypress.env('wiki')
  const space = Cypress.env('space')
  const page = Cypress.env('page')
  const adminUser = Cypress.env('adminUser')
  const adminPass = Cypress.env('adminPass')

  const tagsEndpoint = `/rest/wikis/${wiki}/spaces/${space}/pages/${page}/tags`

  it('Should fetch the tags of a page successfully (GET)', () => {
    cy.request({
      method: 'GET',
      url: tagsEndpoint,
      headers: {
        'Accept': 'application/json'
      }
    }).then((response) => {
      expect(response.status).to.eq(200)
      if (response.body && response.body.tags) {
        expect(response.body).to.have.property('tags')
      }
    })
  })

  it('Should update the tags of a page successfully (PUT)', () => {
    const testTag = `cypress-api-tag-${Date.now()}`

    cy.request({
      method: 'PUT',
      url: tagsEndpoint,
      auth: { user: adminUser, pass: adminPass },
      headers: { 'Accept': 'application/json', 'Content-Type': 'application/json' },
      body: {
        tags: [{ name: testTag }]
      }
    }).then((response) => {
      // 202 ACCEPTED is returned by setTags() in PageTagsResourceImpl
      expect(response.status).to.eq(202)
    })

    // Verify it was saved
    cy.request({
      method: 'GET',
      url: tagsEndpoint,
      headers: { 'Accept': 'application/json' }
    }).then((response) => {
      expect(response.body).to.have.property('tags')
      const tags = response.body.tags.map(t => t.name)
      expect(tags).to.include(testTag)
    })
  })
})
