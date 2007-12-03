-- --------------------------------------------------------------------------------------------
-- Obtains the position of a node within a subtree.
-- 
-- @param _root   the root node
-- @param _nodeId the node to retrieve the position for
-- @param _live   if true the live tree is queried, if false the edit tree
-- --------------------------------------------------------------------------------------------
drop function if exists tree_nodeIndex|
Create function tree_nodeIndex(_root INTEGER UNSIGNED,_nodeId INTEGER UNSIGNED,_live boolean)
returns int deterministic reads sql data
BEGIN
  DECLARE _pos   INTEGER default -1;
  DECLARE _count INTEGER default 0;
  DECLARE _ref   INTEGER UNSIGNED default null;
  DECLARE done BOOLEAN DEFAULT FALSE;

  DECLARE curEdit CURSOR FOR
    select ref from FXS_TREE where
      lft>=(select lft from FXS_TREE where id=_root) and
      rgt<=(select rgt from FXS_TREE where id=_root) order by lft;

  DECLARE curLive CURSOR FOR
    select ref from FXS_TREE_LIVE where
      lft>=(select lft from FXS_TREE_LIVE where id=_root) and
      rgt<=(select rgt from FXS_TREE_LIVE where id=_root) order by lft;

  DECLARE CONTINUE HANDLER FOR SQLSTATE '02000' SET done = TRUE;

  IF (_live=true or _live is null) THEN
    OPEN curLive;
    WHILE NOT done DO
      FETCH curLive INTO _ref;
      if (_ref=_nodeId) then
        set _pos = _count;
        set done = true;
      end if;
      set _count = _count+1;
    END WHILE;
    CLOSE curLive;
  ELSE
    OPEN curEdit;
    WHILE NOT done DO
      FETCH curEdit INTO _ref;
      if (_ref=_nodeId) then
        set _pos = _count;
        set done = true;
      end if;
      set _count = _count+1;
    END WHILE;
    CLOSE curEdit;
  END IF;

  return _pos;
END|
