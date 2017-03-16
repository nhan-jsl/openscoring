package org.openscoring.dao.model;

import javax.persistence.*;
import java.io.Serializable;

/**
 * Created by nhannd on 3/16/17.
 */
@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"permission", "roleName"})})
public class RolesPermission implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column
    private String permission;

    @Column
    private String roleName;

    public RolesPermission(){}

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }
}