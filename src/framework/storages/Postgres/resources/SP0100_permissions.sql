-- /**
--  * This function retrieves the permissions of a given content instance for the given user.
-- * @param _contentId       the content id
--  * @param _contentVersion  the content version
--  * @param _userId          the user to retrieve the permissions for
--  * @return                 the permissions as [0|1] string array.
--  *                         Order: READ|EDIT|DELETE|CREATE|EXPORT|REL
--  *                         1=right granted, 0=right denied
--  *                         Example: '110000' for READ and EDIT granted, everything else denied
--  **/drop function if exists permissions(_contentId INTEGER,_contentVersion INTEGER,_userId INTEGER);
Create OR REPLACE function permissions(_contentId INTEGER,_contentVersion INTEGER,_userId INTEGER) returns varchar(6) AS $$
--deterministic reads sql data
--BEGIN
  DECLARE
	GRP_OWNER INTEGER default 2;
	_result text default '';
  DECLARE done BOOLEAN DEFAULT FALSE;
  DECLARE GLOBAL_SUPERVISOR BOOLEAN DEFAULT FALSE;
  DECLARE MANDATOR_SUPERVISOR BOOLEAN DEFAULT FALSE;
  DECLARE _createdBy INTEGER;
  DECLARE _userGroup INTEGER;
  DECLARE _instanceMandator INTEGER;
  DECLARE _USER_MANDATOR INTEGER default 0;
  DECLARE _role INTEGER;
  DECLARE _type INTEGER;
  DECLARE _edit BOOLEAN;
  DECLARE _delete BOOLEAN;
  DECLARE _export BOOLEAN;
  DECLARE _rel BOOLEAN;
  DECLARE _read BOOLEAN;
  DECLARE _create BOOLEAN;
  -- Total
  DECLARE PREMOVE BOOLEAN DEFAULT FALSE;
  DECLARE PEDIT BOOLEAN DEFAULT FALSE;
  DECLARE PEXPORT BOOLEAN DEFAULT FALSE;
  DECLARE PREL BOOLEAN DEFAULT FALSE;
  DECLARE PREAD BOOLEAN DEFAULT FALSE;
  DECLARE PCREATE BOOLEAN DEFAULT FALSE;
  -- Instance
  DECLARE IPREMOVE BOOLEAN DEFAULT FALSE;
  DECLARE IPEDIT BOOLEAN DEFAULT FALSE;
  DECLARE IPEXPORT BOOLEAN DEFAULT FALSE;
  DECLARE IPREL BOOLEAN DEFAULT FALSE;
  DECLARE IPEXT BOOLEAN DEFAULT FALSE;
  DECLARE IPREAD BOOLEAN DEFAULT FALSE;
  DECLARE IPCREATE BOOLEAN DEFAULT FALSE;
  -- Step
  DECLARE SPREMOVE BOOLEAN DEFAULT FALSE;
  DECLARE SPEDIT BOOLEAN DEFAULT FALSE;
  DECLARE SPEXPORT BOOLEAN DEFAULT FALSE;
  DECLARE SPREL BOOLEAN DEFAULT FALSE;
  DECLARE SPREAD BOOLEAN DEFAULT FALSE;
  DECLARE SPCREATE BOOLEAN DEFAULT FALSE;
  -- Type
  DECLARE TPREMOVE BOOLEAN DEFAULT FALSE;
  DECLARE TPEDIT BOOLEAN DEFAULT FALSE;
  DECLARE TPEXPORT BOOLEAN DEFAULT FALSE;
  DECLARE TPREL BOOLEAN DEFAULT FALSE;
  DECLARE TPREAD BOOLEAN DEFAULT FALSE;
  DECLARE TPCREATE BOOLEAN DEFAULT FALSE;

  -- Role cursor
  DECLARE curRole CURSOR FOR
  select role from FXS_ROLEMAPPING where account=_userId or
    usergroup in (select usergroup from FXS_USERGROUPMEMBERS where account=_userId);

  -- Permission cursor
  DECLARE cur CURSOR FOR
  select
    dat.created_by, ass.usergroup, ass.PEDIT, ass.PREMOVE, ass.PEXPORT, ass.PREL, ass.PREAD, ass.PCREATE, acl.cat_type, dat.mandator
  from
    (select con.mandator, con.step, con.created_by, con.id, con.ver, con.tdef, con.acl, stp.acl stepAcl, typ.acl typeAcl
	from FX_CONTENT con,FXS_TYPEDEF typ, FXS_WF_STEPS stp
	where
	  con.id=_contentId and
	  con.ver=_contentVersion and
	  con.tdef=typ.id and
	  stp.id=con.step)
    dat,
    FXS_ACLASSIGNMENTS ass,
    FXS_ACL acl
  where
    acl.id=ass.acl and
    ass.usergroup in (select usergroup from FXS_USERGROUPMEMBERS where account=_userId union select GRP_OWNER from FXS_USERGROUPMEMBERS) and
    (ass.acl=dat.acl or ass.acl=dat.typeAcl or ass.acl=dat.stepAcl);


BEGIN
  EXCEPTION WHEN SQLSTATE '02000' THEN done = true;
  -- ------------------------------------------------------------------------------------------
  -- READ USER MANDATOR -----------------------------------------------------------------------
  -- ------------------------------------------------------------------------------------------
  select mandator into _USER_MANDATOR from FXS_ACCOUNTS where id=_userId;

  -- ------------------------------------------------------------------------------------------
  -- CHECK ROLES ------------------------------------------------------------------------------
  -- ------------------------------------------------------------------------------------------
  OPEN curRole;
  WHILE NOT done LOOP
    FETCH curRole INTO _role;
    IF (_role=1) THEN GLOBAL_SUPERVISOR=true; END IF;
    IF (_role=2) THEN MANDATOR_SUPERVISOR=true; END IF;
  END LOOP;
  CLOSE curRole;

  -- ------------------------------------------------------------------------------------------
  -- GLOBAL SUPERVISOR ------------------------------------------------------------------------
  -- ------------------------------------------------------------------------------------------
  IF (GLOBAL_SUPERVISOR) THEN
	return '111111';
  END IF;

  -- ------------------------------------------------------------------------------------------
  -- GATHER PERMISSIONS -----------------------------------------------------------------------
  -- ------------------------------------------------------------------------------------------
  done = false;
  OPEN cur;
  WHILE NOT done LOOP
    FETCH cur INTO _createdBy,_userGroup,_edit,_delete,_export,_rel,_read,_create,_type,_instanceMandator;
      CASE _type
          WHEN 1 /*Content*/ THEN
            IF (_userGroup!=GRP_OWNER or (_userGroup=GRP_OWNER and _createdBy=_userId)) THEN
              IF (_edit)   THEN IPEDIT=true;   END IF;
              IF (_delete) THEN IPREMOVE=true; END IF;
              IF (_export) THEN IPEXT=true; END IF;
              IF (_rel)    THEN IPREL=true;    END IF;
              IF (_read)   THEN IPREAD=true;   END IF;
              IF (_create) THEN IPCREATE=true; END IF;
            END IF;
          WHEN 2 /*Type*/ THEN
              IF (_edit)   THEN TPEDIT=true;   END IF;
              IF (_delete) THEN TPREMOVE=true; END IF;
              IF (_export) THEN TPEXPORT=true; END IF;
              IF (_rel)    THEN TPREL=true;    END IF;
              IF (_read)   THEN TPREAD=true;   END IF;
              IF (_create) THEN TPCREATE=true; END IF;
          WHEN 3 /*Workflow Step*/ THEN
              IF (_edit)   THEN SPEDIT=true;   END IF;
              IF (_delete) THEN SPREMOVE=true; END IF;
              IF (_export) THEN SPEXPORT=true; END IF;
              IF (_rel)    THEN SPREL=true;    END IF;
              IF (_read)   THEN SPREAD=true;   END IF;
              IF (_create) THEN SPCREATE=true; END IF;
          ELSE
  	    done=true;
      END CASE;
  END LOOP;
  CLOSE cur;

  -- ------------------------------------------------------------------------------------------
  -- MANDATOR SUPERVISOR ----------------------------------------------------------------------
  -- ------------------------------------------------------------------------------------------
  IF (MANDATOR_SUPERVISOR and _USER_MANDATOR=_instanceMandator) THEN
	return '111111';
  END IF;

  -- ------------------------------------------------------------------------------------------
  -- CONDENSE PERMISSIONS ---------------------------------------------------------------------
  -- ------------------------------------------------------------------------------------------
  -- ------------------------------------------------------------------------------------------
  PEDIT   = IPEDIT and TPEDIT and SPEDIT;
  PREMOVE = IPREMOVE and TPREMOVE and SPREMOVE;
  PEXPORT = IPEXPORT and TPEXPORT and SPEXPORT;
  PREL    = IPREL and TPREL and SPREL;
  PREAD   = IPREAD and TPREAD and SPREAD;
  PCREATE = IPCREATE and TPCREATE and SPCREATE;


  -- ------------------------------------------------------------------------------------------
  -- BUILD RESULT -----------------------------------------------------------------------------
  -- ------------------------------------------------------------------------------------------
  _result = concat(_result,PREAD);
  _result = concat(_result,PEDIT);
  _result = concat(_result,PREMOVE);
  _result = concat(_result,PCREATE);
  _result = concat(_result,PEXPORT);
  _result = concat(_result,PREL);

  return _result;
END;
$$ LANGUAGE 'plpgsql';
