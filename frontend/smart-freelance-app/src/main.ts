import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { App } from './app/app';

// Polyfill for sockjs-client which uses Node.js `global` in browser context
(window as any).global = window;

bootstrapApplication(App, appConfig)
  .catch((err) => console.error(err));
