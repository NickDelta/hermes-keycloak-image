# Importing / Exporting Realms

## Exports
To export the hermes realm execute the following snippet:

```bash
# To be used inside the keycloak container
/opt/jboss/keycloak/bin/standalone.sh \
-Djboss.socket.binding.port-offset=200 -Dkeycloak.migration.action=export \
-Dkeycloak.migration.provider=singleFile \
-Dkeycloak.migration.realmName=hermes \
-Dkeycloak.migration.usersExportStrategy=REALM_FILE \
-Dkeycloak.migration.file=/opt/jboss/keycloak/imports/hermes-export.json
```

## Imports
Imports are handled on container starts.