-- Get a chain of id's for the given node (from the root node) like /1/4/42 for node 42
drop function if exists tree_idchain|
Create function tree_idchain(_nodeId INTEGER UNSIGNED,live boolean)
returns text deterministic reads sql data
BEGIN
  DECLARE _result text default '';
  DECLARE _id INTEGER UNSIGNED default null;
  DECLARE done BOOLEAN DEFAULT FALSE;
  DECLARE CONTINUE HANDLER FOR SQLSTATE '02000' SET done = TRUE;

  IF _nodeId=1 THEN
    return '/1';
  END IF;

  WHILE NOT done DO

    SET _id = null;

    IF live THEN
      select parent into _id from FXS_TREE_LIVE where id=_nodeId;
    ELSE
      select parent into _id from FXS_TREE where id=_nodeId;
    END IF;

    IF _id IS NOT NULL THEN
      set _result = concat(_nodeId,_result);
      set _result = concat("/",_result);
    END IF;

    IF _id=1 THEN
      set _result = concat("/1",_result);
    END IF;

    set _nodeId = _id;

  END WHILE;

  return _result;
END|