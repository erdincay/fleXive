-- --------------------------------------------------------------------------------------------
-- Obtains the position of a node within a subtree.
--
-- @param _root   the root node
-- @param _nodeId the node to retrieve the position for
-- @param _live   if true the live tree is queried, if false the edit tree
-- --------------------------------------------------------------------------------------------
Create OR REPLACE function tree_nodeIndex(_root INTEGER,_nodeId INTEGER,_live boolean)
returns int AS $$ -- deterministic reads sql data
DECLARE
  _pos   INTEGER default -1;
  _count INTEGER default 0;
  _ref   INTEGER default null;
  done BOOLEAN DEFAULT FALSE;

  curEdit CURSOR FOR
    select ref from FXS_TREE where
      lft>=(select lft from FXS_TREE where id=_root) and
      rgt<=(select rgt from FXS_TREE where id=_root) order by lft;

  curLive CURSOR FOR
    select ref from FXS_TREE_LIVE where
      lft>=(select lft from FXS_TREE_LIVE where id=_root) and
      rgt<=(select rgt from FXS_TREE_LIVE where id=_root) order by lft;

BEGIN
  IF (_live=true or _live is null) THEN
    OPEN curLive;
    WHILE NOT done LOOP
      FETCH curLive INTO _ref;
      IF NOT FOUND THEN
        done = TRUE;
      END IF;
      IF (_ref=_nodeId) THEN
        _pos = _count;
        done = true;
      END IF;
      _count = _count+1;
    END LOOP;
    CLOSE curLive;
  ELSE
    OPEN curEdit;
    WHILE NOT done LOOP
      FETCH curEdit INTO _ref;
      IF NOT FOUND THEN
        done = TRUE;
      END IF;
      IF (_ref=_nodeId) THEN
        _pos = _count;
        done = true;
      END IF;
      _count = _count+1;
    END LOOP;
    CLOSE curEdit;
  END IF;

  RETURN _pos;
END;
$$ LANGUAGE 'plpgsql';
