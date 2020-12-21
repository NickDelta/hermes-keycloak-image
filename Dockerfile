FROM jboss/keycloak:12.0.1

COPY keycloak-hermes-customizations/keycloak-deployer-ear/target/hermes-customizations.ear /opt/jboss/keycloak/standalone/deployments/hermes-customizations.ear
COPY realm-config /opt/jboss/keycloak/imports
CMD ["-b 0.0.0.0", "-Dkeycloak.import=/opt/jboss/keycloak/imports/hermes-export.json"]
