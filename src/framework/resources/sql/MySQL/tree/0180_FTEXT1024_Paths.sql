-- --------------------------------------------------------------------------------------------
-- Obtains all paths for a given instance id.
-- The path elements are separated by a '/' character, and every element is encoded: 
--   <displayName>:<treeNodeId>:<refId>
-- @param _contentId the instance id
-- @param _lang      the language to retrieve
-- @param _tprop     the property to use for the path (must be of type FTEXT1024)
-- @param _live      true:  read from the live tree,
--       false: read from the edit tree, 
--       null:  read from both trees (paths will be returned double if contained in both trees)
-- --------------------------------------------------------------------------------------------
drop function if exists tree_FTEXT1024_Paths|
Create function tree_FTEXT1024_Paths(_contentId INTEGER UNSIGNED,
	_lang INTEGER,_tprop INTEGER UNSIGNED,_live boolean)
returns text CHARSET UTF8 deterministic reads sql data
BEGIN
  DECLARE _result text default '';
  DECLARE _path text;
  DECLARE done BOOLEAN DEFAULT FALSE;
  DECLARE curLive CURSOR FOR 
     select tree_FTEXT1024_Chain(id,_lang,_tprop,true) chain from FXS_TREE_LIVE where ref=_contentId order by chain;
  DECLARE curMax CURSOR FOR 
     select tree_FTEXT1024_Chain(id,_lang,_tprop,false) chain from FXS_TREE where ref=_contentId order by chain;
  DECLARE CONTINUE HANDLER FOR SQLSTATE '02000' SET done = TRUE;

  IF (_live=true or _live is null) THEN 
    OPEN curLive;
    WHILE NOT done DO
      FETCH curLive INTO _path;
      if NOT done THEN
        if (_result!='') then
          set _result = concat(_result,'\n');
        end if;
          set _result = concat(_result,_path);
      END IF;
    END WHILE;
    CLOSE curLive;
  END IF;

  IF (_live=false or _live is null) THEN 
    set done=false;
    OPEN curMax;
    WHILE NOT done DO
      FETCH curMax INTO _path;
      if NOT done THEN
        if (_result!='') then
          set _result = concat(_result,'\n');
        end if;
          set _result = concat(_result,_path);
      END IF;
    END WHILE;
    CLOSE curMax;
  END IF;

  return _result;
END|
