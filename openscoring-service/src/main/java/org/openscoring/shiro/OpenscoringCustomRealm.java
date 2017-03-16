package org.openscoring.shiro;

import org.apache.shiro.authc.*;
import org.apache.shiro.realm.jdbc.JdbcRealm;
import org.openscoring.dao.UserDAO;
import org.openscoring.dao.model.User;

/**
 * Created by nhannd on 3/16/17.
 */
public class OpenscoringCustomRealm extends JdbcRealm {
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        // identify account to log to
        UsernamePasswordToken userPassToken = (UsernamePasswordToken) token;
        final String username = userPassToken.getUsername();

        if (username == null) {
            System.out.println("Username is null.");
            return null;
        }

        // read password hash and salt from db
        final User user = UserDAO.getUserByUsername(username);

        if (user == null) {
            System.out.println("No account found for user [" + username + "]");
            return null;
        }

        // return salted credentials
        SaltedAuthenticationInfo info =
                new OpenscoringSaltedAuthentificationInfo(username, user.getPassword(), user.getSalt());

        return info;
    }
}
