// cypress/support/commands.js

Cypress.Commands.add('login', (username, password) => {
  cy.session([username, password], () => {
    cy.visit('/bin/login/XWiki/XWikiLogin', { failOnStatusCode: false })
    cy.get('#j_username').type(username)
    cy.get('#j_password').type(password)
    cy.get('input[type="submit"]').click()
    cy.url().should('not.include', '/login')
  })
})

Cypress.Commands.add('apiAuthHeaders', (username, password) => {
  return {
    'Authorization': 'Basic ' + btoa(`${username}:${password}`),
    'Accept': 'application/json',
    'Content-Type': 'application/json'
  }
})
