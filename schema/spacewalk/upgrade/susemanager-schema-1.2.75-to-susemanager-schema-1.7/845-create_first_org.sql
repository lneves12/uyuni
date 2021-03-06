create or replace procedure
create_first_org
(
	name_in in varchar2,
	password_in in varchar2
) is
	ug_type			number;
	group_val		number;
begin
	insert into web_customer (
		id, name
	) values (
		1, name_in
	);

	select rhn_user_group_id_seq.nextval into group_val from dual;

	select	id
	into	ug_type
	from	rhnUserGroupType
	where	label = 'org_admin';

	insert into rhnUserGroup (
		id, name,
		description,
		max_members, group_type, org_id
	) values (
		group_val, 'Organization Administrators',
		'Organization Administrators for Org ' || name_in || ' (1)',
		NULL, ug_type, 1
	);

	select rhn_user_group_id_seq.nextval into group_val from dual;

	select	id
	into	ug_type
	from	rhnUserGroupType
	where	label = 'org_applicant';

	insert into rhnUserGroup (
		id, name,
		description,
		max_members, group_type, org_id
	) VALues (
		group_val, 'Organization Applicants',
		'Organization Applicants for Org ' || name_in || ' (1)',
		NULL, ug_type, 1
	);

	select rhn_user_group_id_seq.nextval into group_val from dual;

	select	id
	into	ug_type
	from	rhnUserGroupType
	where	label = 'system_group_admin';

	insert into rhnUserGroup (
		id, name,
		description,
		max_members, group_type, org_id
	) values (
		group_val, 'System Group Administrators',
		'System Group Administrators for Org ' || name_in || ' (1)',
		NULL, ug_type, 1
	);


	select rhn_user_group_id_seq.nextval into group_val from dual;

	select	id
	into	ug_type
	from	rhnUserGroupType
	where	label = 'activation_key_admin';

	insert into rhnUserGroup (
		id, name,
		description,
		max_members, group_type, org_id
	) values (
		group_val, 'Activation Key Administrators',
		'Activation Key Administrators for Org ' || name_in || ' (1)',
		NULL, ug_type, 1
	);

	-- config admin is special; it gets created in
	-- rhn_entitlements.set_customer_provisioning instead.
	
	select rhn_user_group_id_seq.nextval into group_val from dual;

	select	id
	into	ug_type
	from	rhnUserGroupType
	where	label = 'channel_admin';

	insert into rhnUserGroup (
		id, name,
		description,
		max_members, group_type, org_id
	) values (
		group_val, 'Channel Administrators',
		'Channel Administrators for Org ' || name_in || ' (1)',
		NULL, ug_type, 1
	);

	select rhn_user_group_id_seq.nextval into group_val from dual;

	select	id
	into	ug_type
	from	rhnUserGroupType
	where	label = 'satellite_admin';

	insert into rhnUserGroup (
		id, name,
		description,
		max_members, group_type, org_id
	) values (
		group_val, 'SUSE Manager Administrators',
		'SUSE Manager Administrators for Org ' || name_in || ' (1)',
		NULL, ug_type, 1
	);


	-- there aren't any users yet, so we don't need to update
	-- rhnUserServerPerms
        insert into rhnServerGroup 
		( id, name, description, max_members, group_type, org_id )
		select rhn_server_group_id_seq.nextval, sgt.name, sgt.name, 
			0, sgt.id, 1
		from rhnServerGroupType sgt
		where sgt.label = 'sw_mgr_entitled';
		
end create_first_org;
/
show errors;

