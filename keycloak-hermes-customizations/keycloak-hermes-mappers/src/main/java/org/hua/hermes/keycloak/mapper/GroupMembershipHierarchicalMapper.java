package org.hua.hermes.keycloak.mapper;

import org.hua.hermes.keycloak.mapper.entity.HierarchicalGroup;
import org.keycloak.models.GroupModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.*;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.IDToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:nikosdelta@protonmail.com">Nick Dimitrakopoulos</a>
 */
public class GroupMembershipHierarchicalMapper extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper, OIDCIDTokenMapper, UserInfoTokenMapper
{

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    static {
        OIDCAttributeMapperHelper.addTokenClaimNameConfig(configProperties);

       ProviderConfigProperty property1 = new ProviderConfigProperty();
        property1.setName("tree.level");
        property1.setLabel("Max Tree Level");
        property1.setType(ProviderConfigProperty.STRING_TYPE);
        property1.setHelpText("Maximum level of the group hierarchy tree. " +
                "You can also set it to MAX and it will map the whole hierarchy.");
        configProperties.add(property1);

        OIDCAttributeMapperHelper.addIncludeInTokensConfig(configProperties, GroupMembershipHierarchicalMapper.class);
    }

    public static final String PROVIDER_ID = "oidc-group-membership-hierarchical-mapper";


    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Group Membership Hierarchy";
    }

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Map user group membership (with hierarchical capabilities). " +
                "Created by Nick Dimitrakopoulos for the Hermes project.";
    }


    protected void setClaim(IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession) {

        String levelStr = mappingModel.getConfig().get("tree.level");
        int level = (levelStr.equals("MAX")) ? Integer.MAX_VALUE : Integer.parseInt(levelStr);

        List<HierarchicalGroup> groups = userSession.getUser()
                .getGroupsStream()
                .map(group -> getGroupHierarchy(group,level))
                .collect(Collectors.toList());

        String protocolClaim = mappingModel.getConfig().get(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME);
        token.getOtherClaims().put(protocolClaim, groups);
    }

    public HierarchicalGroup getGroupHierarchy(GroupModel groupModel, int maxLevel)
    {
        GroupModel parent = groupModel.getParent();
        if(parent == null || maxLevel <= 1)
            return new HierarchicalGroup(
                    groupModel.getId(),
                    groupModel.getName(),
                    groupModel.getRoleMappingsStream()
                            .map(RoleModel::getName)
                            .collect(Collectors.toList()),
                    null);
        else
            return new HierarchicalGroup(
                    groupModel.getId(),
                    groupModel.getName(),
                    groupModel.getRoleMappingsStream()
                            .map(RoleModel::getName)
                            .collect(Collectors.toList()),
                    getGroupHierarchy(parent,maxLevel - 1));
    }

    public static ProtocolMapperModel create(String name,
                                             String tokenClaimName,
                                             boolean consentRequired, String consentText,
                                             boolean accessToken, boolean idToken) {
        ProtocolMapperModel mapper = new ProtocolMapperModel();
        mapper.setName(name);
        mapper.setProtocolMapper(PROVIDER_ID);
        mapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        Map<String, String> config = new HashMap<String, String>();
        config.put(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, tokenClaimName);
        if (accessToken) config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true");
        if (idToken) config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "true");
        mapper.setConfig(config);

        return mapper;
    }
}