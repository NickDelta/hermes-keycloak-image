package org.hua.hermes.keycloak.rest;

import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.GroupResource;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.Path;

/**
 * @author <a href="mailto:nikosdelta@protonmail.com">Nick Dimitrakopoulos</a>
 */
public class OrganizationResource
{
    private final KeycloakSession session;
    private final RealmModel realm;
    private final AdminPermissionEvaluator auth;
    private final AdminEventBuilder adminEvent;
    private final GroupModel group;

    public OrganizationResource(KeycloakSession session, RealmModel realm,
                                AdminPermissionEvaluator auth, AdminEventBuilder adminEvent, GroupModel group)
    {
        this.session = session;
        this.realm = realm;
        this.auth = auth;
        this.adminEvent = adminEvent;
        this.group = group;
    }

    @Path("/manage")
    public GroupResource manage(){
        GroupResource resource = new GroupResource(realm,group,session,auth,adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(resource);
        return resource;
    }

    @Path("/employees")
    public OrganizationEmployeesResource employees(){

        GroupModel employeesGroup = group.getSubGroupsStream()
                .filter(g -> g.getName().equals("EMPLOYEES"))
                .findFirst()
                .orElseThrow(InternalServerErrorException::new);

        OrganizationEmployeesResource resource = new OrganizationEmployeesResource(session,realm, auth, adminEvent, employeesGroup);
        ResteasyProviderFactory.getInstance().injectProperties(resource);
        return resource;
    }

    @Path("/supervisors")
    public OrganizationSupervisorsResource supervisors(){
        GroupModel employeesGroup = group.getSubGroupsStream()
                .filter(g -> g.getName().equals("SUPERVISORS"))
                .findFirst()
                .orElseThrow(InternalServerErrorException::new);

        OrganizationSupervisorsResource resource = new OrganizationSupervisorsResource(session,realm, auth, adminEvent, employeesGroup);
        ResteasyProviderFactory.getInstance().injectProperties(resource);
        return resource;
    }

}
