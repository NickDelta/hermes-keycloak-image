FROM jboss/keycloak:13.0.0

COPY realm-config /opt/jboss/keycloak/imports
COPY keycloak-hermes-customizations/keycloak-deployer-ear/target/hermes-customizations.ear /opt/jboss/keycloak/standalone/deployments/hermes-customizations.ear
CMD ["-b 0.0.0.0", "-Dkeycloak.migration.action=import", "-Dkeycloak.migration.provider=singleFile", "-Dkeycloak.migration.file=/opt/jboss/keycloak/imports/hermes-export.json", "-Dkeycloak.migration.strategy=IGNORE_EXISTING"]
