FROM quay.io/keycloak/keycloak:26.6.0

# Copy custom configuration (no hardcoded credentials)
COPY conf/keycloak.conf /opt/keycloak/conf/keycloak.conf

# Enable health checks
ENV KC_HEALTH_ENABLED=true
ENV KC_HTTP_ENABLED=true

ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
CMD ["start-dev"]
