export const environment = {
  production: false,
  /** API Gateway (8078) – all requests go here; gateway routes to keycloak-auth (8079), user, etc. */
  apiGatewayUrl: 'http://localhost:8078',
  authApiPrefix: 'keycloak-auth/api/auth',
  /** Set at build time if you use ElevenLabs STT; never commit real keys. */
  elevenLabsApiKey: '',
  /** Chat assistant offres, badge « IA », micro dictée, tests de compétences IA — désactivé par défaut */
  showAiUi: false,
};
