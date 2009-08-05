/**
 * This function returns true if the user has read access on the given instance.
 * Same as permissions(__), but faster since more data has to be specified when calling.
 *
 * @param _contentId           the content id
 * @param _contentVersion      the content version
 * @param _userId              the user to retrieve the permissions for
 * @param _USER_MANDATOR       the mandator that the user belongs to
 * @param MANDATOR_SUPERVISOR  true if the user is a mandator supervisor
 * @param GLOBAL_SUPERVISOR    true if the user is a global supervisor
 * @return                     true if read permission is granted
 **/
drop function if exists permissions2|
Create function permissions2(_contentId INTEGER UNSIGNED,_contentVersion INTEGER UNSIGNED,_userId INTEGER UNSIGNED,
  _USER_MANDATOR INTEGER UNSIGNED,MANDATOR_SUPERVISOR BOOLEAN,GLOBAL_SUPERVISOR BOOLEAN)
returns varchar(6) deterministic reads sql data
BEGIN
  DECLARE GRP_OWNER INTEGER UNSIGNED default 2;
  DECLARE _result text default '';
  DECLARE done BOOLEAN DEFAULT FALSE;
  DECLARE _createdBy INTEGER UNSIGNED;
  DECLARE _userGroup INTEGER UNSIGNED;
  DECLARE _instanceMandator INTEGER UNSIGNED;
  DECLARE _role INTEGER UNSIGNED;
  DECLARE _type INTEGER UNSIGNED;
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

  -- Permission cursor
  DECLARE cur CURSOR FOR
  select
    dat.created_by,ass.usergroup,ass.PEDIT,ass.PREMOVE,ass.PEXPORT,ass.PREL,ass.PREAD,ass.PCREATE,acl.cat_type,dat.mandator
  from
    (select con.mandator,con.step,con.created_by,con.id,con.ver,con.tdef,con.acl,stp.acl stepAcl,typ.acl typeAcl
	from FX_CONTENT con,FXS_TYPEDEF typ, FXS_WF_STEPS stp where con.id=_contentId and con.ver=_contentVersion and
	con.tdef=typ.id and stp.id=con.step) dat,
    FXS_ACLASSIGNMENTS ass,
    FXS_ACL acl
  where
    acl.id=ass.acl and
    ass.usergroup in (select usergroup from FXS_USERGROUPMEMBERS where account=_userId union select GRP_OWNER from FXS_USERGROUPMEMBERS) and
    (ass.acl=dat.acl or ass.acl=dat.typeAcl or ass.acl=dat.stepAcl);
  DECLARE CONTINUE HANDLER FOR SQLSTATE '02000' SET done = TRUE;

  -- ------------------------------------------------------------------------------------------
  -- GLOBAL SUPERVISOR ------------------------------------------------------------------------
  -- ------------------------------------------------------------------------------------------
  IF (GLOBAL_SUPERVISOR) THEN
	return '111111';
  END IF;

  -- ------------------------------------------------------------------------------------------
  -- GATHER PERMISSIONS -----------------------------------------------------------------------
  -- ------------------------------------------------------------------------------------------
  set done=false;
  OPEN cur;
  WHILE NOT done DO
    FETCH cur INTO _createdBy,_userGroup,_edit,_delete,_export,_rel,_read,_create,_type,_instanceMandator;
      CASE _type
          WHEN 1 /*Content*/ THEN
            IF (_userGroup!=GRP_OWNER or (_userGroup=GRP_OWNER and _createdBy=_userId)) THEN
              IF (_edit)   THEN set IPEDIT=true;   END IF;
              IF (_delete) THEN set IPREMOVE=true; END IF;
              IF (_export) THEN set IPEXPORT=true; END IF;
              IF (_rel)    THEN set IPREL=true;    END IF;
              IF (_read)   THEN set IPREAD=true;   END IF;
              IF (_create) THEN set IPCREATE=true; END IF;
            END IF;
          WHEN 2 /*Type*/ THEN
              IF (_edit)   THEN set TPEDIT=true;   END IF;
              IF (_delete) THEN set TPREMOVE=true; END IF;
              IF (_export) THEN set TPEXPORT=true; END IF;
              IF (_rel)    THEN set TPREL=true;    END IF;
              IF (_read)   THEN set TPREAD=true;   END IF;
              IF (_create) THEN set TPCREATE=true; END IF;
          WHEN 3 /*Workflow Step*/ THEN
              IF (_edit)   THEN set SPEDIT=true;   END IF;
              IF (_delete) THEN set SPREMOVE=true; END IF;
              IF (_export) THEN set SPEXPORT=true; END IF;
              IF (_rel)    THEN set SPREL=true;    END IF;
              IF (_read)   THEN set SPREAD=true;   END IF;
              IF (_create) THEN set SPCREATE=true; END IF;
          ELSE
  	    set done=true;
      END CASE;
  END WHILE;
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
  set PEDIT   = IPEDIT and TPEDIT and SPEDIT;
  set PREMOVE = IPREMOVE and TPREMOVE and SPREMOVE;
  set PEXPORT = IPEXPORT and TPEXPORT and SPEXPORT;
  set PREL    = IPREL and TPREL and SPREL;
  set PREAD   = IPREAD and TPREAD and SPREAD;
  set PCREATE = IPCREATE and TPCREATE and SPCREATE;


  -- ------------------------------------------------------------------------------------------
  -- BUILD RESULT -----------------------------------------------------------------------------
  -- ------------------------------------------------------------------------------------------
  set _result = concat(_result,PREAD);
  set _result = concat(_result,PEDIT);
  set _result = concat(_result,PREMOVE);
  set _result = concat(_result,PCREATE);
  set _result = concat(_result,PEXPORT);
  set _result = concat(_result,PREL);

  return _result;

END|
