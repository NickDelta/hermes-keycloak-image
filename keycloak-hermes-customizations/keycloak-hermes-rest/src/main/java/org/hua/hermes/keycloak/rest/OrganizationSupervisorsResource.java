package org.hua.hermes.keycloak.rest;

import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.JpaUserProvider;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.GroupResource;
import org.keycloak.services.resources.admin.UserResource;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

import javax.persistence.EntityManager;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:nikosdelta@protonmail.com">Nick Dimitrakopoulos</a>
 */
public class OrganizationSupervisorsResource
{
    private final KeycloakSession session;
    private final RealmModel realm;
    private final EntityManager em;
    private final AdminPermissionEvaluator auth;
    private final AdminEventBuilder adminEvent;
    private final GroupModel supervisorsGroup;

    public OrganizationSupervisorsResource(KeycloakSession session, RealmModel realm,
                                           AdminPermissionEvaluator auth, AdminEventBuilder adminEvent,
                                           GroupModel supervisorsGroup)
    {
        this.session = session;
        this.realm = realm;
        this.em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        this.auth = auth;
        this.adminEvent = adminEvent;
        this.supervisorsGroup = supervisorsGroup;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Stream<UserRepresentation> list(@QueryParam("offset") Integer offset,
                                           @QueryParam("limit") Integer limit) {
        auth.users().requireView();

        return session.users().getGroupMembersStream(realm, supervisorsGroup, offset, limit)
                .map(user -> ModelToRepresentation.toRepresentation(session, realm, user));
    }

    @GET
    @Path("/count")
    @Produces(MediaType.APPLICATION_JSON)
    public Integer count() {
        auth.users().requireView();

        JpaUserProvider userProvider = new JpaUserProvider(session,em);

        Set<String> groupIds = new HashSet<>();
        groupIds.add(supervisorsGroup.getId());

        return userProvider.getUsersCount(realm, groupIds);
    }

    @Path("/manage")
    public GroupResource manage(){
        GroupResource resource = new GroupResource(realm,supervisorsGroup,session,auth,adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(resource);
        return resource;
    }

    @Path("/{id}")
    public UserResource supervisor(@PathParam("id") String userId) {
        auth.users().requireManage();
        UserModel user = session.users()
                .getGroupMembersStream(realm, supervisorsGroup)
                .filter(u -> u.getId().equals(userId))
                .findFirst().orElseThrow(NotFoundException::new);

        UserResource resource = new UserResource(realm,user,auth,adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(resource);
        return resource;
    }

}
