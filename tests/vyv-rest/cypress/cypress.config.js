const { defineConfig } = require("cypress");

module.exports = defineConfig({
  e2e: {
    baseUrl: "http://localhost:8080",
    setupNodeEvents(on, config) {
      // implement node event listeners here
    },
    env: {
      wiki: "xwiki",
      space: "Sandbox",
      page: "WebHome",
      adminUser: "emmanuel_calad",
      adminPass: "Joaco06151970_",
      guestUser: "Guest",
      guestPass: "guest"
    },
    video: false,
    screenshotOnRunFailure: true
  },
});
