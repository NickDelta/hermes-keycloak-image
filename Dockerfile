FROM jboss/keycloak:11.0.3

COPY realm-config /opt/jboss/keycloak/imports
CMD ["-b 0.0.0.0", "-Dkeycloak.import=/opt/jboss/keycloak/imports/hermes-export.json"]
