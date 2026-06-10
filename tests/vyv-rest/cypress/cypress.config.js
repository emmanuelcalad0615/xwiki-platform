const { defineConfig } = require("cypress");

module.exports = defineConfig({
  e2e: {
    baseUrl: "http://localhost:8080",
    setupNodeEvents(on, config) {
      // implement node event listeners here
    },
    env: {
      wiki: "xwiki",
      space: "Main",
      page: "WebHome",
      adminUser: "admin",
      adminPass: "12345678",
      guestUser: "Guest",
      guestPass: "guest"
    },
    video: false,
    screenshotOnRunFailure: true
  },
});
