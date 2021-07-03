FROM jboss/keycloak:14.0.0

# Copy realm export file
COPY realm-config /opt/jboss/keycloak/imports

# Copy customizations
COPY keycloak-hermes-customizations/keycloak-deployer-ear/target/hermes-customizations.ear /opt/jboss/keycloak/standalone/deployments/hermes-customizations.ear

# Add healthcheck
HEALTHCHECK --interval=10s --timeout=40s \
  CMD curl -f http://localhost:8080/auth/realms/master || exit 1

CMD [ "-Dkeycloak.migration.action=import", "-Dkeycloak.migration.provider=singleFile", "-Dkeycloak.migration.file=/opt/jboss/keycloak/imports/hermes-export.json", "-Dkeycloak.migration.strategy=IGNORE_EXISTING" ]
