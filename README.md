# Hermes Keycloak Image

## Purpose

This repository aims to produce a customized image of Keycloak,
capable of handling the special requirements of Hermes.

Note that Hermes is an **educational project**.

## External Dependencies

To be fully operational, this service requires:

- A MySQL database instance.

## Environmental Variables

See the official documentation [here](https://github.com/keycloak/keycloak-containers/tree/master/server#environment-variables).

## Observability

Keycloak does not currently provide observability out of the box. In addition, it is currently 
a little complex to add an observability module like [smallrye-health](https://github.com/thomasdarimont/keycloak-extension-playground/tree/master/smallrye-health-extension),
so no effort was made to integrate such an ability. In the (near) future, the Keycloak dev community plans
to support health checks without any config, see [KEYCLOAK-12398](https://issues.redhat.com/browse/KEYCLOAK-12398) for more details.

This means that, the k8s config of this project only supports a `readiness` probe as shown in the official [example](https://www.keycloak.org/getting-started/getting-started-kube) :

```yaml
readinessProbe:
  httpGet:
    path: /auth/realms/master
    port: 8080
```

## About The Realm Files

It is generally not recommended putting realm config exports inside an image as it contains very sensitive data
that must remain secret. Since this is an **educational project**, there is no concern. 

## Deployment Options

There are 2 deployment options:

- A `docker-compose.yaml` file can be found [here](https://github.com/NickDelta/hermes-deployment/blob/main/local_deployment/files/docker-compose.yml). It will deploy the whole application along with the backend.
- `Kubernetes` manifest files can also be found under `/k8s`.


## Contributors

- Nick Dimitrakopoulos ([GitHub](https://github.com/NickDelta))
- Thanos Apostolides ([GitHub](https://github.com/apostolides))
