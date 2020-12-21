package org.hua.hermes.keycloak.rest;

import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class CustomRestResource
{

    private final KeycloakSession session;
    private final AuthenticationManager.AuthResult auth;

    public CustomRestResource(KeycloakSession session) {
        this.session = session;
        this.auth = new AppAuthManager.BearerTokenAuthenticator(session).authenticate();
    }

    @GET
    @Path("/groups")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getGroupHierarchyByPath(@QueryParam("path") String path,
                                            @QueryParam("full") boolean full)
    {
        checkRealmAdmin();
        GroupModel groupModel = KeycloakModelUtils.findGroupByPath(session.getContext().getRealm(),path);
        if(groupModel != null)
            return Response.ok(ModelToRepresentation.toGroupHierarchy(groupModel,full)).build();
        else
            return Response.status(Response.Status.NOT_FOUND).build();

    }

    private void checkRealmAdmin()
    {
        if (auth == null) {
            throw new NotAuthorizedException("Bearer");
        } else if (auth.getToken().getRealmAccess() == null ||
                !auth.getToken().getResourceAccess()
                        .get("realm-management")
                        .getRoles()
                        .contains("query-groups")) {
            throw new ForbiddenException("Does not have realm admin role");
        }
    }
}
