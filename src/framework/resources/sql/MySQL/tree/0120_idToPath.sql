-- Get the path (excluding the root node) for a node
drop function if exists tree_idToPath|
Create function tree_idToPath(nodeId INTEGER UNSIGNED,live boolean)
returns text deterministic reads sql data
BEGIN
  DECLARE result text;
  DECLARE name text;
  DECLARE done BOOLEAN DEFAULT FALSE;
  DECLARE cur CURSOR FOR
    SELECT parent.name FROM FXS_TREE AS node, FXS_TREE AS parent
    WHERE node.lft>=parent.lft and node.lft<=parent.rgt AND node.id=nodeId
    ORDER BY parent.lft;
  DECLARE curLive CURSOR FOR
    SELECT parent.name FROM FXS_TREE_LIVE AS node, FXS_TREE_LIVE AS parent
    WHERE node.lft>=parent.lft and node.lft<=parent.rgt AND node.id=nodeId
    ORDER BY parent.lft;
  DECLARE CONTINUE HANDLER FOR SQLSTATE '02000' SET done = TRUE;

  IF nodeId=1 THEN
    return '/';
  END IF;

  IF live THEN
    OPEN curLive;
  ELSE
    OPEN cur;
  END IF;

  set name = '';
  set result = '';
  WHILE NOT done DO

    IF live THEN
      FETCH curLive INTO name;
    ELSE
      FETCH cur INTO name;
    END IF;

    if NOT done THEN
      set result = concat(result,"/");
      set result = concat(result,name);
    END IF;

  END WHILE;

  set result = substring(result,6); 

  IF live THEN
      CLOSE curLive;
  ELSE
      CLOSE cur;
  END IF;

  return result;
END|



