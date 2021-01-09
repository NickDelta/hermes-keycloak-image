package org.hua.hermes.keycloak.rest;

import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.common.ClientConnection;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.*;
import org.keycloak.models.jpa.GroupAdapter;
import org.keycloak.models.jpa.entities.GroupEntity;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.permissions.AdminPermissions;

import javax.persistence.EntityManager;
import javax.ws.rs.*;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:nikosdelta@protonmail.com">Nick Dimitrakopoulos</a>
 */
public class OrganizationsResource
{

    private final KeycloakSession session;
    private final RealmModel realm;
    private final EntityManager em;
    private final ClientConnection clientConnection;
    private final AdminPermissionEvaluator auth;
    private final AdminEventBuilder adminEvent;

    private final GroupModel organizationsGroup;

    public OrganizationsResource(KeycloakSession session) {
        this.session = session;
        this.realm = session.getContext().getRealm();
        this.em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        this.clientConnection = session.getContext().getConnection();

        AdminAuth adminAuth = authenticateRealmAdminRequest(session.getContext().getRequestHeaders());
        this.auth = AdminPermissions.evaluator(session, realm, adminAuth);
        this.adminEvent = new AdminEventBuilder(realm, adminAuth, session, clientConnection);
        this.organizationsGroup = findOrganizationsGroup();
    }


    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public List<GroupRepresentation> list(@QueryParam("offset") Integer offset,
                                          @QueryParam("limit")  Integer limit)
    {
        auth.groups().requireView();

        //As far as I know, there isn't any ready implementation to get subgroups with paginated queries
        //So I made one myself
        return em.createQuery("SELECT g FROM GroupEntity g WHERE g.parentId = :parentId", GroupEntity.class)
                .setParameter("parentId",organizationsGroup.getId())
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultStream()
                .map(entity -> (GroupModel)new GroupAdapter(realm,em,entity))
                .map(model -> ModelToRepresentation.toRepresentation(model,true))
                .collect(Collectors.toList());

    }

    @GET
    @Path("/count")
    @Produces({MediaType.APPLICATION_JSON})
    public Integer count()
    {
        auth.groups().requireList();

        //As far as I know, there isn't any ready implementation to count the subgroups of a group
        //Only by fetching all the subgroups, a count was possible. That's costly.
        //So a raw count query is the best solution I can think of.
        return em.createQuery("SELECT COUNT(g.id) FROM GroupEntity g WHERE g.parentId = :id",Long.class)
                .setParameter("id",organizationsGroup.getId())
                .getSingleResult().intValue();
    }

    @Path("/{name}")
    public OrganizationResource organization(@PathParam("name") String name) {
        auth.groups().requireManage();

        GroupModel organization = organizationsGroup.getSubGroupsStream()
                .filter(org -> org.getName().equals(name))
                .findFirst()
                .orElseThrow(NotFoundException::new);

        OrganizationResource resource = new OrganizationResource(session,realm, auth, adminEvent, organization);
        ResteasyProviderFactory.getInstance().injectProperties(resource);
        return resource;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(String orgName){

        //Very fragile endpoint code-wise. Do not use this in production!!
        auth.groups().requireManage();

        GroupModel orgGroup = realm.createGroup(orgName, organizationsGroup);

        GroupModel employeesGroup = realm.createGroup("EMPLOYEES",orgGroup);

        //We need entity not model and I haven't found any smarter way of obtaining it by a model
        //So i fetch it again with EntityManager
        GroupEntity empGroupEntity = em.createQuery("SELECT g FROM GroupEntity g WHERE g.id = :id", GroupEntity.class)
                .setParameter("id", employeesGroup.getId())
                .getSingleResult();

        GroupAdapter adapter = new GroupAdapter(realm,em,empGroupEntity);

        RoleModel orgEmployeeRole = realm.getRole("ROLE_ORG_EMPLOYEE");
        if(orgEmployeeRole == null) throw new InternalServerErrorException("ROLE_ORG_EMPLOYEE does not exist");
        adapter.grantRole(orgEmployeeRole);

        GroupModel supervisorsGroup = realm.createGroup("SUPERVISORS",orgGroup);

        //Same thing here
        GroupEntity supervisorsGroupEntity = em.createQuery("SELECT g FROM GroupEntity g WHERE g.id = :id", GroupEntity.class)
                .setParameter("id", supervisorsGroup.getId())
                .getSingleResult();

        adapter = new GroupAdapter(realm,em,supervisorsGroupEntity);

        RoleModel orgSupervisorRole = realm.getRole("ROLE_ORG_SUPERVISOR");
        if(orgSupervisorRole == null) throw new InternalServerErrorException("ROLE_ORG_SUPERVISOR does not exist");
        adapter.grantRole(orgSupervisorRole);

        GroupRepresentation childRep = ModelToRepresentation.toGroupHierarchy(orgGroup, true);

        return Response.status(200).type(MediaType.APPLICATION_JSON_TYPE).entity(childRep).build();
    }

    protected AdminAuth authenticateRealmAdminRequest(HttpHeaders headers) {
        String tokenString = AppAuthManager.extractAuthorizationHeaderToken(headers);
        if (tokenString == null) throw new NotAuthorizedException("Bearer");
        AccessToken token;
        try {
            JWSInput input = new JWSInput(tokenString);
            token = input.readJsonContent(AccessToken.class);
        } catch (JWSInputException e) {
            throw new NotAuthorizedException("Bearer token format error");
        }
        String realmName = token.getIssuer().substring(token.getIssuer().lastIndexOf('/') + 1);
        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = realmManager.getRealmByName(realmName);
        if (realm == null) {
            throw new NotAuthorizedException("Unknown realm in token");
        }
        session.getContext().setRealm(realm);

        AuthenticationManager.AuthResult authResult = new AppAuthManager.BearerTokenAuthenticator(session)
                .setRealm(realm)
                .setConnection(clientConnection)
                .setHeaders(headers)
                .authenticate();

        if (authResult == null) {
            //logger.debug("Token not valid");
            throw new NotAuthorizedException("Bearer");
        }

        ClientModel client = realm.getClientByClientId(token.getIssuedFor());
        if (client == null) {
            throw new NotFoundException("Could not find client for authorization");

        }

        return new AdminAuth(realm, authResult.getToken(), authResult.getUser(), client);
    }

    private GroupModel findOrganizationsGroup(){
        GroupModel group = KeycloakModelUtils.findGroupByPath(realm, "ORGANIZATIONS");
        if(group == null){
            throw new InternalServerErrorException("ORGANIZATIONS group does not exist");
        }
        return group;
    }

}
