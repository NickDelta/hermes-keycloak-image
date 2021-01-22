package org.hua.hermes.keycloak.rest;

import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.JpaUserProvider;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.GroupResource;
import org.keycloak.services.resources.admin.UserResource;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

import javax.persistence.EntityManager;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:nikosdelta@protonmail.com">Nick Dimitrakopoulos</a>
 */
public class OrganizationEmployeesResource
{
    private final KeycloakSession session;
    private final RealmModel realm;
    private final EntityManager em;
    private final AdminPermissionEvaluator auth;
    private final AdminEventBuilder adminEvent;
    private final GroupModel employeesGroup;

    public OrganizationEmployeesResource(KeycloakSession session, RealmModel realm,
                                         AdminPermissionEvaluator auth, AdminEventBuilder adminEvent,
                                         GroupModel employeesGroup)
    {
        this.session = session;
        this.realm = realm;
        this.em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        this.auth = auth;
        this.adminEvent = adminEvent;
        this.employeesGroup = employeesGroup;
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Stream<UserRepresentation> list(@QueryParam("offset") Integer offset,
                                           @QueryParam("limit") Integer limit) {
        auth.users().requireView();

        return session.users().getGroupMembersStream(realm, employeesGroup, offset, limit)
                .map(user -> ModelToRepresentation.toRepresentation(session, realm, user));
    }

    @GET
    @Path("/count")
    @Produces({MediaType.APPLICATION_JSON})
    public Integer count() {
        auth.users().requireView();

        JpaUserProvider userProvider = new JpaUserProvider(session,em);

        Set<String> groupIds = new HashSet<>();
        groupIds.add(employeesGroup.getId());

        return userProvider.getUsersCount(realm, groupIds);
    }

    @Path("/{id}")
    public UserResource employee(@PathParam("id") String userId) {
        auth.users().requireManage();

        UserModel user = session.users()
                .getGroupMembersStream(realm, employeesGroup)
                .filter(u -> u.getId().equals(userId))
                .findFirst()
                .orElseThrow(NotFoundException::new);

        UserResource resource = new UserResource(realm,user,auth,adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(resource);
        return resource;

    }

    @Path("/manage")
    public GroupResource manage(){
        GroupResource resource = new GroupResource(realm,employeesGroup,session,auth,adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(resource);
        return resource;
    }

}
