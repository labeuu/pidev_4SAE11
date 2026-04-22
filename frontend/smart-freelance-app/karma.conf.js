// Karma configuration — used by `ng test` (@angular/build:karma). CI: ChromeHeadless + codeCoverage via angular.json.
// When CHROME_BIN is not set (typical in Jenkins/headless Linux), use Chromium from the puppeteer package so `ng test` does
// not require a system Chrome. ChromeHeadlessCI adds flags often required in container/CI sandboxes.
if (!process.env.CHROME_BIN) {
  try {
    const puppeteer = require('puppeteer');
    if (puppeteer.executablePath) {
      process.env.CHROME_BIN = puppeteer.executablePath();
    }
  } catch {
    // puppeteer not installed; karma-chrome-launcher will use system Chrome if available
  }
}

module.exports = function (config) {
  config.set({
    basePath: '',
    frameworks: ['jasmine'],
    plugins: [
      require('karma-jasmine'),
      require('karma-chrome-launcher'),
      require('karma-jasmine-html-reporter'),
      require('karma-coverage'),
    ],
    client: {
      jasmine: {},
      clearContext: false,
    },
    jasmineHtmlReporter: {
      suppressAll: true,
    },
    reporters: ['progress', 'kjhtml'],
    browsers: ['Chrome'],
    customLaunchers: {
      ChromeHeadlessCI: {
        base: 'ChromeHeadless',
        flags: [
          '--no-sandbox',
          '--disable-gpu',
          '--disable-dev-shm-usage',
        ],
      },
    },
    restartOnFileChange: true,
  });
};
