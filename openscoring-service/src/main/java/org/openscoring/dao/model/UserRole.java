package org.openscoring.dao.model;

import javax.persistence.*;
import java.io.Serializable;

/**
 * Created by nhannd on 3/16/17.
 */
@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"roleName", "username"})})
public class UserRole implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column
    private String roleName;

    @Column
    private String username;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}