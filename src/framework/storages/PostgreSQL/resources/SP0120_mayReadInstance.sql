/**
 * This function returns true if the user has read access on the given instance.
 * @param _contentId       the content id
 * @param _contentVersion  the content version
 * @param _userId          the user to retrieve the permissions for
 * @return                 true if read permission is granted
 **/
Create OR REPLACE function mayReadInstance(_contentId INTEGER,_contentVersion INTEGER,_userId INTEGER)
returns BOOLEAN AS $$ -- TODO deterministic reads sql data
  DECLARE codedPerms varchar(6) default '';
BEGIN
  return substr(permissions(_contentId,_contentVersion,_userId),1,1)='1';
END;
$$ LANGUAGE 'plpgsql';
