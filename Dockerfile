FROM jboss/keycloak:12.0.2

COPY keycloak-hermes-customizations/keycloak-deployer-ear/target/hermes-customizations.ear /opt/jboss/keycloak/standalone/deployments/hermes-customizations.ear
COPY realm-config /opt/jboss/keycloak/imports
CMD ["-b 0.0.0.0", "-Dkeycloak.migration.action=import", "-Dkeycloak.migration.provider=singleFile", "-Dkeycloak.migration.file=/opt/jboss/keycloak/imports/hermes-export.json", "-Dkeycloak.migration.strategy=OVERWRITE_EXISTING"]
