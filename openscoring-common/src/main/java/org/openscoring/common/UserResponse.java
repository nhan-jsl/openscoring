package org.openscoring.common;

import java.io.Serializable;

/**
 * Created by nhannd on 3/12/17.
 */
public class UserResponse implements Serializable {
    private String username;
    private String password;

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
}
