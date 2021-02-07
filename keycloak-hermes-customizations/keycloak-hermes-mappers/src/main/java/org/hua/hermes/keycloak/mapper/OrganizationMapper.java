package org.hua.hermes.keycloak.mapper;

import org.hua.hermes.keycloak.mapper.representation.SummarizedGroup;
import org.keycloak.models.*;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.mappers.*;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.IDToken;

import java.util.*;

/**
 * @author <a href="mailto:nikosdelta@protonmail.com">Nick Dimitrakopoulos</a>
 */
public class OrganizationMapper
        extends AbstractOIDCProtocolMapper
        implements OIDCAccessTokenMapper, OIDCIDTokenMapper, UserInfoTokenMapper
{

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    public static final String PROVIDER_ID = "oidc-group-membership-hierarchical-mapper";

    static {
        OIDCAttributeMapperHelper.addTokenClaimNameConfig(configProperties);
        OIDCAttributeMapperHelper.addIncludeInTokensConfig(configProperties, OrganizationMapper.class);
    }


    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Organization Mapper";
    }

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Puts organization info in the token (Only for Supervisors And Employees)";
    }


    protected void setClaim(IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession) {

        UserModel user = userSession.getUser();
        Set<RoleModel> roles = new HashSet<>();
        roles.add(KeycloakModelUtils.getRoleFromString(userSession.getRealm(),"ROLE_ORG_SUPERVISOR"));
        roles.add(KeycloakModelUtils.getRoleFromString(userSession.getRealm(),"ROLE_ORG_EMPLOYEE"));

        boolean worksInOrganization = user.getGroupsStream()
                .flatMap(RoleMapperModel::getRoleMappingsStream)
                .anyMatch(roles::contains);

        if(!worksInOrganization)
            return;

        //We assume that a correct setup is made here.
        //If the role is not assigned to a group or
        //that group does not have a parent, this will fail miserably.
        GroupModel group = userSession.getUser()
                .getGroupsStream()
                .findFirst()
                .get()
                .getParent();


        String protocolClaim = mappingModel.getConfig().get(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME);
        token.getOtherClaims().put(protocolClaim, new SummarizedGroup(group.getId(),group.getName()));
    }
}