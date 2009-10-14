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
CREATE OR REPLACE FUNCTION tree_FTEXT1024_Paths(_contentId BIGINT,
	_lang BIGINT,_tprop BIGINT,_live BOOLEAN)
returns text AS $$ -- CHARSET UTF8 deterministic reads sql data
DECLARE
  _result TEXT DEFAULT '';
  _path TEXT;
  done BOOLEAN DEFAULT FALSE;
  curLive CURSOR FOR
     SELECT tree_FTEXT1024_Chain(id,_lang,_tprop,true) _chain FROM FXS_TREE_LIVE WHERE ref=_contentId ORDER BY _chain;
  curMax CURSOR FOR
     SELECT tree_FTEXT1024_Chain(id,_lang,_tprop,false) _chain FROM FXS_TREE WHERE ref=_contentId ORDER BY _chain;
BEGIN
  IF (_live=true OR _live IS NULL) THEN
    OPEN curLive;
    WHILE NOT done LOOP
      FETCH curLive INTO _path;
      IF NOT FOUND THEN
        done = TRUE;
      END IF;
      IF NOT done THEN
        IF (_result!='') THEN
          _result = _result || "\n";
        END IF;
          _result = _result || _path;
      END IF;
    END LOOP;
    CLOSE curLive;
  END IF;

  IF (_live=false OR _live IS NULL) THEN
    done=false;
    OPEN curMax;
    WHILE NOT done LOOP
      FETCH curMax INTO _path;
      IF NOT FOUND THEN
        done = TRUE;
      END IF;
      IF NOT done THEN
        IF (_result!='') THEN
          _result = _result || "\n";
        END IF;
          _result = _result || _path;
      END IF;
    END LOOP;
    CLOSE curMax;
  END IF;

  RETURN _result;
END;
$$ LANGUAGE 'plpgsql';
