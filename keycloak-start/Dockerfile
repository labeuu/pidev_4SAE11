FROM quay.io/keycloak/keycloak:26.6.0

# Copy custom configuration (no hardcoded credentials)
COPY keycloak-start/conf/keycloak.conf /opt/keycloak/conf/keycloak.conf
COPY keycloak/smart-freelance-realm.json /opt/keycloak/data/import/smart-freelance-realm.json

# Enable health checks
ENV KC_HEALTH_ENABLED=true
ENV KC_HTTP_ENABLED=true

ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
CMD ["start-dev", "--import-realm"]
