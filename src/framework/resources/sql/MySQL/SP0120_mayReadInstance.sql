/**
 * This function returns true if the user has read access on the given instance.
 * @param _contentId       the content id
 * @param _contentVersion  the content version
 * @param _userId          the user to retrieve the permissions for
 * @return                 true if read permission is granted
 **/
drop function if exists mayReadInstance|
Create function mayReadInstance(_contentId INTEGER UNSIGNED,_contentVersion INTEGER UNSIGNED,_userId INTEGER UNSIGNED)
returns BOOLEAN deterministic reads sql data
BEGIN
  DECLARE codedPerms varchar(6) default '';
  return substr(permissions(_contentId,_contentVersion,_userId),1,1)='1';  
END|
