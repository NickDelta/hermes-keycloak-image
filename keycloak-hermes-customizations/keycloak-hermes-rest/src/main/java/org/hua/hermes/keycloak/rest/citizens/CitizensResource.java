package org.hua.hermes.keycloak.rest.citizens;

import org.keycloak.common.ClientConnection;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.*;
import org.keycloak.models.jpa.JpaUserProvider;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.GroupResource;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.permissions.AdminPermissions;

import javax.persistence.EntityManager;
import javax.ws.rs.*;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:nikosdelta@protonmail.com">Nick Dimitrakopoulos</a>
 */
public class CitizensResource
{
    private final KeycloakSession session;
    private final RealmModel realm;
    private final EntityManager em;
    private final ClientConnection clientConnection;
    private final AdminPermissionEvaluator auth;
    private final AdminEventBuilder adminEvent;

    private final GroupModel citizensGroup;

    public CitizensResource(KeycloakSession session) {
        this.session = session;
        this.realm = session.getContext().getRealm();
        this.clientConnection = session.getContext().getConnection();
        this.em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        AdminAuth adminAuth = authenticateRealmAdminRequest(session.getContext().getRequestHeaders());
        this.auth = AdminPermissions.evaluator(session, realm, adminAuth);
        this.adminEvent = new AdminEventBuilder(realm, adminAuth, session, clientConnection);
        this.citizensGroup = findCitizensGroup();
    }

    @Path("/manage")
    public GroupResource manage() {
        return new GroupResource(realm,citizensGroup,session,auth,adminEvent);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Stream<UserRepresentation> list(@QueryParam("offset") Integer offset,
                                           @QueryParam("limit") Integer limit) {
        auth.users().requireView();

        return session.users().getGroupMembersStream(realm, citizensGroup, offset, limit)
                .map(user -> ModelToRepresentation.toRepresentation(session, realm, user));
    }

    @GET
    @Path("/count")
    @Produces({MediaType.APPLICATION_JSON})
    public Integer count() {
        auth.users().requireView();

        JpaUserProvider userProvider = new JpaUserProvider(session,em);

        Set<String> groupIds = new HashSet<>();
        groupIds.add(citizensGroup.getId());

        return userProvider.getUsersCount(realm, groupIds);
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

    private GroupModel findCitizensGroup(){
        GroupModel group = KeycloakModelUtils.findGroupByPath(realm, "CITIZENS");
        if(group == null){
            throw new InternalServerErrorException("CITIZENS group does not exist");
        }
        return group;
    }

}
