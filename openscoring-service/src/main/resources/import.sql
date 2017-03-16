/* create admin account */
insert into `User`(username, password, salt, orgId) values ('admin', 'mR1qycc1EIyFa/RMETTVo13Yxl5mPQVjAdqDAecxBfI=', 'uEAsAoqFJldFB6tXKZGTjQ==', 'openscoring');
insert into RolesPermission(permission, rolename) values ('*', 'admin');
insert into UserRole(roleName, username) values ('admin', 'admin');