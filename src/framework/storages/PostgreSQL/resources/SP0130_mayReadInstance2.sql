/**
 * This function returns true if the user has read access on the given instance.
 *
 * @param _contentId           the content id
 * @param _contentVersion      the content version
 * @param _userId              the user to retrieve the permissions for
 * @param _USER_MANDATOR       the mandator that the user belongs to
 * @param MANDATOR_SUPERVISOR  true if the user is a mandator supervisor
 * @param GLOBAL_SUPERVISOR    true if the user is a global supervisor
 * @return                     true if read permission is granted
 **/
CREATE OR REPLACE function mayReadInstance2(_contentId BIGINT,_contentVersion INTEGER, _userId BIGINT,
  _USER_MANDATOR BIGINT, MANDATOR_SUPERVISOR BOOLEAN,GLOBAL_SUPERVISOR BOOLEAN)
returns BOOLEAN AS $$ -- TODO deterministic reads sql data
  DECLARE GRP_OWNER BIGINT default 2;
  DECLARE _result text default '';
  DECLARE done BOOLEAN DEFAULT FALSE;
  DECLARE _createdBy BIGINT;
  DECLARE _userGroup BIGINT;
  DECLARE _instanceMandator BIGINT;
  DECLARE _role INTEGER;
  DECLARE _type BIGINT;
  DECLARE _read BOOLEAN;
  DECLARE _securityMode INTEGER;
  -- Instance
  DECLARE IPREAD BOOLEAN DEFAULT FALSE;
  -- Step
  DECLARE SPREAD BOOLEAN DEFAULT FALSE;
  -- Type
  DECLARE TPREAD BOOLEAN DEFAULT FALSE;

  -- Permission cursor - type, step and first content ACL
  DECLARE cur CURSOR FOR

  select
    dat.created_by,ass.usergroup,ass.PREAD,acl.cat_type,dat.mandator,dat.securityMode
  from
    (select con.mandator,con.created_by,con.acl,stp.acl stepAcl,typ.acl typeAcl,typ.security_mode securityMode
	from FX_CONTENT con,FXS_TYPEDEF typ, FXS_WF_STEPS stp where con.id=_contentId and con.ver=_contentVersion and
	con.tdef=typ.id and stp.id=con.step) dat,
    FXS_ACLASSIGNMENTS ass,
    FXS_ACL acl
  where
    acl.id=ass.acl and
    ass.usergroup in (select usergroup from FXS_USERGROUPMEMBERS where account=_userId union select GRP_OWNER) and
    (ass.acl=dat.acl or ass.acl=dat.typeAcl or ass.acl=dat.stepAcl)

  UNION

  select
    dat.created_by,ass.usergroup,ass.PREAD,acl.cat_type,dat.mandator,dat.securityMode
  from
    (select con.mandator,con.created_by,con.acl,stp.acl stepAcl,typ.acl typeAcl,typ.security_mode securityMode
	from FX_CONTENT con,FXS_TYPEDEF typ, FXS_WF_STEPS stp where con.id=_contentId and con.ver=_contentVersion and
	con.tdef=typ.id and stp.id=con.step) dat,
    (select ca.acl from FX_CONTENT_ACLS ca where ca.id=_contentId and ca.ver=_contentVersion) contentACLs,
    FXS_ACLASSIGNMENTS ass,
    FXS_ACL acl
  where
    acl.id=ass.acl and
    ass.usergroup in (select usergroup from FXS_USERGROUPMEMBERS where account=_userId union select GRP_OWNER) and
    ass.acl=contentACLs.acl;
BEGIN
  -- ------------------------------------------------------------------------------------------
  -- GLOBAL SUPERVISOR ------------------------------------------------------------------------
  -- ------------------------------------------------------------------------------------------
  IF (GLOBAL_SUPERVISOR) THEN
	return true;
  END IF;

  -- ------------------------------------------------------------------------------------------
  -- GATHER PERMISSIONS -----------------------------------------------------------------------
  -- ------------------------------------------------------------------------------------------
  done=false;
  OPEN cur;
  WHILE NOT done LOOP
    FETCH cur INTO _createdBy,_userGroup,_read,_type,_instanceMandator,_securityMode;
    IF NOT FOUND THEN
      done = TRUE;
    END IF;
      IF (_securityMode & 1 = 0) THEN
        IPREAD=true;    /* content permissions are disabled */
      END IF;
      IF (_securityMode & 8 = 0) THEN
        TPREAD=true;  /* type permissions are disabled */
      END IF;
      IF (_securityMode & 4 = 0) THEN
        SPREAD=true;  /* workflow step permissions are disabled */
      END IF;
      CASE _type
          WHEN 1 /*Content*/ THEN
            IF (_userGroup!=GRP_OWNER OR (_userGroup=GRP_OWNER AND _createdBy=_userId)) THEN
              IF (_read)   THEN IPREAD=true;   END IF;
            END IF;
          WHEN 2 /*Type*/ THEN
              IF (_read)   THEN TPREAD=true;   END IF;
          WHEN 3 /*Workflow Step*/ THEN
              IF (_read)   THEN SPREAD=true;   END IF;
          ELSE
  	    done=true;
      END CASE;
      IF (IPREAD and TPREAD and SPREAD) THEN
        CLOSE cur;
        RETURN TRUE;    /* break out early */
      END IF;
  END LOOP;
  CLOSE cur;

  -- ------------------------------------------------------------------------------------------
  -- MANDATOR SUPERVISOR ----------------------------------------------------------------------
  -- ------------------------------------------------------------------------------------------
  IF (MANDATOR_SUPERVISOR and _USER_MANDATOR=_instanceMandator) THEN
	return true;
  END IF;

  -- ------------------------------------------------------------------------------------------
  -- CONDENSE AND RETURN  RESULT --------------------------------------------------------------
  -- ------------------------------------------------------------------------------------------
  return IPREAD and TPREAD and SPREAD;

END;
$$ LANGUAGE 'plpgsql';
