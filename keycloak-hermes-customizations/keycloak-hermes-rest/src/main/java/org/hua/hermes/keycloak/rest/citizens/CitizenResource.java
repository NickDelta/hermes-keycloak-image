package org.hua.hermes.keycloak.rest.citizens;

import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.UserResource;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

/**
 * @author <a href="mailto:nikosdelta@protonmail.com">Nick Dimitrakopoulos</a>
 */
public class CitizenResource
{
    private final KeycloakSession session;
    private final RealmModel realm;
    private final AdminPermissionEvaluator auth;
    private final AdminEventBuilder adminEvent;
    private final GroupModel citizensGroup;

    public CitizenResource(KeycloakSession session, RealmModel realm,
                           AdminPermissionEvaluator auth, AdminEventBuilder adminEvent,
                           GroupModel citizensGroup)
    {
        this.session = session;
        this.realm = realm;
        this.auth = auth;
        this.adminEvent = adminEvent;
        this.citizensGroup = citizensGroup;
    }

    @Path("/{id}")
    public UserResource citizen(@PathParam("id") String userId) {
        auth.users().requireManage();

        UserModel user = session.users()
                .getGroupMembersStream(realm, citizensGroup)
                .filter(u -> u.getId().equals(userId))
                .findFirst()
                .orElseThrow(NotFoundException::new);

        UserResource resource = new UserResource(realm,user,auth,adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(resource);
        return resource;
    }

}
