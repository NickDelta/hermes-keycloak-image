package org.hua.hermes.keycloak.mapper.entity;

import java.util.List;

/**
 * @author <a href="mailto:nikosdelta@protonmail.com">Nick Dimitrakopoulos</a>
 */
public class HierarchicalGroup
{
    private String id;
    private String name;
    private List<String> groupRoles;
    private HierarchicalGroup parent;

    public HierarchicalGroup(String id, String name, List<String> groupRoles, HierarchicalGroup parent)
    {
        this.id = id;
        this.name = name;
        this.groupRoles = groupRoles;
        this.parent = parent;
    }

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public List<String> getGroupRoles()
    {
        return groupRoles;
    }

    public void setGroupRoles(List<String> groupRoles)
    {
        this.groupRoles = groupRoles;
    }

    public HierarchicalGroup getParent()
    {
        return parent;
    }

    public void setParent(HierarchicalGroup parent)
    {
        this.parent = parent;
    }
}
