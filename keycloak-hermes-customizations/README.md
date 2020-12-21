# Customizing Keycloak

This project packages an .ear file that contains a series of provider implementations which are deployed to
Keycloak using [Keycloak Deployer](https://www.keycloak.org/docs/latest/server_development/index.html#using-the-keycloak-deployer).

It consists of:

- [keycloak-deployer-ear](keycloak-deployer-ear):
  The module that is responsible for the .ear packaging.
- [keycloak-hermes-mappers](keycloak-hermes-mappers):
  The module that registers some custom [mappers](https://www.keycloak.org/docs/latest/server_admin/#_protocol-mappers). **Related issues:** ( #2 )
- [keycloak-hermes-rest](keycloak-hermes-rest):
  The modules that registers some [custom REST endpoints](https://www.keycloak.org/docs/latest/server_development/index.html#_extensions_rest). **Related issues:** ( #3 )