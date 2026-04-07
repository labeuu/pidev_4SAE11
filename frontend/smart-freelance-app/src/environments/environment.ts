export const environment = {
  production: false,
  /** API Gateway (8078) – all requests go here; gateway routes to keycloak-auth (8079), user, etc. */
  apiGatewayUrl: 'http://localhost:8078',
  authApiPrefix: 'keycloak-auth/api/auth',
  elevenLabsApiKey: 'sk_1c50fa72d84123a3aa1ce081dac0f0e4ddb437f4343e5585',
  /** Chat assistant offres, badge « IA », micro dictée, tests de compétences IA — désactivé par défaut */
  showAiUi: false,
};
