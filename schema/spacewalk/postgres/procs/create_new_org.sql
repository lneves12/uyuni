-- oracle equivalent source sha1 ad73b94e4013d13c8abbce0a8f0988854fe019fd
--
-- Copyright (c) 2008--2012 Red Hat, Inc.
--
-- This software is licensed to you under the GNU General Public License,
-- version 2 (GPLv2). There is NO WARRANTY for this software, express or
-- implied, including the implied warranties of MERCHANTABILITY or FITNESS
-- FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
-- along with this software; if not, see
-- http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
-- 
-- Red Hat trademarks are not licensed under GPLv2. No permission is
-- granted to use or replicate Red Hat trademarks that are incorporated
-- in this software or its documentation. 
--

create or replace function create_new_org
(
        name_in      in varchar,
        password_in  in varchar
        --org_id_out   out number
) returns numeric
as
$$
declare
        ug_type                 numeric;
        group_val               numeric;
        new_org_id              numeric;
        org_id_out		numeric;
begin

        select nextval('web_customer_id_seq') into new_org_id;

        insert into web_customer (
                id, name
        ) values (
                new_org_id, name_in
        );

        select nextval('rhn_user_group_id_seq') into group_val;

        select  id
        into    ug_type
        from    rhnUserGroupType
        where   label = 'org_admin';

        insert into rhnUserGroup (
                id, name,
                description,
                max_members, group_type, org_id
        ) values (
                group_val, 'Organization Administrators',
                'Organization Administrators for Org ' || name_in,
                NULL, ug_type, new_org_id
        );

        select nextval('rhn_user_group_id_seq') into group_val;

        select  id
        into    ug_type
        from    rhnUserGroupType
        where   label = 'system_group_admin';

        insert into rhnUserGroup (
                id, name,
                description,
                max_members, group_type, org_id
        ) values (
                group_val, 'System Group Administrators',
                'System Group Administrators for Org ' || name_in,
                NULL, ug_type, new_org_id
        );


        select nextval('rhn_user_group_id_seq') into group_val;

        select  id
        into    ug_type
        from    rhnUserGroupType
        where   label = 'activation_key_admin';

        insert into rhnUserGroup (
                id, name,
                description,
                max_members, group_type, org_id
        ) values (
                group_val, 'Activation Key Administrators',
                'Activation Key Administrators for Org ' || name_in,
                NULL, ug_type, new_org_id
        );

        select nextval('rhn_user_group_id_seq') into group_val;

        select  id
        into    ug_type
        from    rhnUserGroupType
        where   label = 'channel_admin';

        insert into rhnUserGroup (
                id, name,
                description,
                max_members, group_type, org_id
        ) values (
                group_val, 'Channel Administrators',
                'Channel Administrators for Org ' || name_in,
                NULL, ug_type, new_org_id
        );

        select nextval('rhn_user_group_id_seq') into group_val from dual;

        select  id
        into    ug_type
        from    rhnUserGroupType
        where   label = 'config_admin';

        insert into rhnUserGroup (
                id, name,
                description,
                max_members, group_type, org_id
        ) values (
                group_val, 'Configuration Administrators',
                'Configuration Administrators for Org ' || name_in,
                NULL, ug_type, new_org_id
        );

        -- there aren't any users yet, so we don't need to update
        -- rhnUserServerPerms

        insert into rhnServerGroup
                ( id, name, description, group_type, org_id )
                select nextval('rhn_server_group_id_seq'), sgt.name, sgt.name,
                        sgt.id, new_org_id
                from rhnServerGroupType sgt
                where sgt.label = 'bootstrap_entitled';

        insert into rhnServerGroup
                ( id, name, description, group_type, org_id )
                select nextval('rhn_server_group_id_seq'), sgt.name, sgt.name,
                        sgt.id, new_org_id
                from rhnServerGroupType sgt
                where sgt.label = 'enterprise_entitled';

        insert into rhnServerGroup
                ( id, name, description, group_type, org_id )
                select nextval('rhn_server_group_id_seq'), sgt.name, sgt.name,
                        sgt.id, new_org_id
                from rhnServerGroupType sgt
                where sgt.label = 'virtualization_host';

        -- set org entitlements
        insert into rhnOrgEntitlements (org_id, entitlement_id)
               select new_org_id, oet.id from rhnOrgEntitlementType oet
                where oet.label = 'sw_mgr_enterprise';

        insert into rhnOrgEntitlements (org_id, entitlement_id)
               select new_org_id, oet.id from rhnOrgEntitlementType oet
                where oet.label = 'rhn_virtualization';

        org_id_out := new_org_id;

	-- Returning the value of OUT parameter
        return org_id_out;

end;
$$
language plpgsql;


