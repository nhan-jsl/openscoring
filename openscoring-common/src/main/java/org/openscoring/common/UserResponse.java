package org.openscoring.common;

import java.io.Serializable;

/**
 * Created by nhannd on 3/12/17.
 */
public class UserResponse implements Serializable {
    private String username;
    private String password;
    private String orgId;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }
}
