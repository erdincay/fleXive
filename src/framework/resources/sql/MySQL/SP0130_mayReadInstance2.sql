/**
 * This function returns true if the user has read access on the given instance.
 * Same as mayReadInstance(__), but faster since more data has to be specified when calling. 
 *
 * @param _contentId           the content id
 * @param _contentVersion      the content version
 * @param _userId              the user to retrieve the permissions for
 * @param _USER_MANDATOR       the mandator that the user belongs to
 * @param MANDATOR_SUPERVISOR  true if the user is a mandator supervisor
 * @param GLOBAL_SUPERVISOR    true if the user is a global supervisor
 * @return                     true if read permission is granted
 **/
drop function if exists mayReadInstance2|
Create function mayReadInstance2(_contentId INTEGER UNSIGNED,_contentVersion INTEGER UNSIGNED,_userId INTEGER UNSIGNED,
  _USER_MANDATOR INTEGER UNSIGNED,MANDATOR_SUPERVISOR BOOLEAN,GLOBAL_SUPERVISOR BOOLEAN)
returns BOOLEAN deterministic reads sql data
BEGIN
  DECLARE GRP_OWNER INTEGER UNSIGNED default 2;
  DECLARE _result text default '';
  DECLARE done BOOLEAN DEFAULT FALSE;
  DECLARE _createdBy INTEGER UNSIGNED;
  DECLARE _userGroup INTEGER UNSIGNED;
  DECLARE _instanceMandator INTEGER UNSIGNED;
  DECLARE _role INTEGER UNSIGNED;
  DECLARE _type INTEGER UNSIGNED;
  DECLARE _read BOOLEAN;
  -- Instance
  DECLARE IPREAD BOOLEAN DEFAULT FALSE;
  -- Step
  DECLARE SPREAD BOOLEAN DEFAULT FALSE;
  -- Type
  DECLARE TPREAD BOOLEAN DEFAULT FALSE;

  -- Permission cursor
  DECLARE cur CURSOR FOR
  select
    dat.created_by,ass.usergroup,ass.PREAD,acl.cat_type,dat.mandator
  from
    (select con.mandator,con.step,con.created_by,con.id,con.ver,con.tdef,con.acl,stp.acl stepAcl,typ.acl typeAcl
	from FX_CONTENT con,FXS_TYPEDEF typ, FXS_WF_STEPS stp where con.id=_contentId and con.ver=_contentVersion and
	con.tdef=typ.id and stp.id=con.step) dat,
    FXS_ACLASSIGNMENTS ass,
    FXS_ACL acl
  where
    acl.id=ass.acl and
    ass.usergroup in (select usergroup from FXS_USERGROUPMEMBERS where account=_userId union select GRP_OWNER) and
    (ass.acl=dat.acl or ass.acl=dat.typeAcl or ass.acl=dat.stepAcl);
  DECLARE CONTINUE HANDLER FOR SQLSTATE '02000' SET done = TRUE;

  -- ------------------------------------------------------------------------------------------
  -- GLOBAL SUPERVISOR ------------------------------------------------------------------------
  -- ------------------------------------------------------------------------------------------
  IF (GLOBAL_SUPERVISOR) THEN
	return true;
  END IF;

  -- ------------------------------------------------------------------------------------------
  -- GATHER PERMISSIONS -----------------------------------------------------------------------
  -- ------------------------------------------------------------------------------------------
  set done=false;
  OPEN cur;
  WHILE NOT done DO
    FETCH cur INTO _createdBy,_userGroup,_read,_type,_instanceMandator;
      CASE _type
          WHEN 1 /*Content*/ THEN
            IF (_userGroup!=GRP_OWNER or (_userGroup=GRP_OWNER and _createdBy=_userId)) THEN
              IF (_read)   THEN set IPREAD=true;   END IF;
            END IF;
          WHEN 2 /*Type*/ THEN
              IF (_read)   THEN set TPREAD=true;   END IF;
          WHEN 3 /*Workflow Step*/ THEN
              IF (_read)   THEN set SPREAD=true;   END IF;
          ELSE
  	    set done=true;
      END CASE;
  END WHILE;
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

END|
