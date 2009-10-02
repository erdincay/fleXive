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
Create OR REPLACE function tree_FTEXT1024_Paths(_contentId INTEGER,
	_lang INTEGER,_tprop INTEGER,_live boolean)
returns text AS $$ -- CHARSET UTF8 deterministic reads sql data
DECLARE
  _result text default '';
  _path text;
  done BOOLEAN DEFAULT FALSE;
  curLive CURSOR FOR
     select tree_FTEXT1024_Chain(id,_lang,_tprop,true) _chain from FXS_TREE_LIVE where ref=_contentId order by _chain;
  curMax CURSOR FOR
     select tree_FTEXT1024_Chain(id,_lang,_tprop,false) _chain from FXS_TREE where ref=_contentId order by _chain;
BEGIN
  EXCEPTION WHEN SQLSTATE '02000' THEN done = true;

  IF (_live=true or _live is null) THEN
    OPEN curLive;
    WHILE NOT done LOOP
      FETCH curLive INTO _path;
      if NOT done THEN
        if (_result!='') then
          _result = concat(_result,"\n");
        end if;
          _result = concat(_result,_path);
      END IF;
    END LOOP;
    CLOSE curLive;
  END IF;

  IF (_live=false or _live is null) THEN
    done=false;
    OPEN curMax;
    WHILE NOT done LOOP
      FETCH curMax INTO _path;
      if NOT done THEN
        if (_result!='') then
          _result = concat(_result,"\n");
        end if;
          _result = concat(_result,_path);
      END IF;
    END LOOP;
    CLOSE curMax;
  END IF;

  return _result;
END;
$$ LANGUAGE 'plpgsql';
