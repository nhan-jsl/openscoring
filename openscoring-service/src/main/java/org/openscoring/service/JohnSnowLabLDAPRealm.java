package org.openscoring.service;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.realm.ldap.DefaultLdapRealm;
import org.apache.shiro.realm.ldap.LdapContextFactory;
import org.apache.shiro.realm.ldap.LdapUtils;
import org.apache.shiro.subject.PrincipalCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import javax.naming.ldap.LdapContext;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by nhannd on 4/8/17.
 */
public class JohnSnowLabLDAPRealm extends DefaultLdapRealm {

    private static final Logger log = LoggerFactory.getLogger(JohnSnowLabLDAPRealm.class);

    private Set<String> roles = new HashSet<>();

    @Override
    protected AuthenticationInfo queryForAuthenticationInfo(AuthenticationToken token, LdapContextFactory ldapContextFactory)
            throws NamingException {

        Object principal = token.getPrincipal();
        Object credentials = token.getCredentials();

        log.debug("Authenticating user '{}' through LDAP", principal);

        principal = getLdapPrincipal(token);

        LdapContext ctx = null;
        try {
            ctx = ldapContextFactory.getLdapContext(principal, credentials);
            //context was opened successfully, which means their credentials were valid.
            roles = new HashSet<>(); // re-init roles
            String[] attrs = getUserDn(principal.toString()).split(",");
            for (String attr : attrs) {
                String[] prop = attr.split("=");
                if (prop[0].equalsIgnoreCase("ou")){
                    roles.add(prop[1]);
                }
            }

            //Return the AuthenticationInfo:
            return createAuthenticationInfo(token, principal, credentials, ctx);
        } finally {
            LdapUtils.closeContext(ctx);
        }
    }

    @Override
    protected AuthorizationInfo queryForAuthorizationInfo(PrincipalCollection principals,
                                                          LdapContextFactory ldapContextFactory) throws NamingException {
        return new SimpleAuthorizationInfo(roles);
    }

    public void refeshRole(){
        this.roles = new HashSet<>();
    }
}
