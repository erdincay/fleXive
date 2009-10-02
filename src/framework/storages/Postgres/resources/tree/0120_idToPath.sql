-- Get the path (excluding the root node) for a node
Create OR REPLACE function tree_idToPath(nodeId INTEGER,live boolean)
returns text AS $$ -- deterministic reads sql data
DECLARE
   result text;
   name text;
   done BOOLEAN DEFAULT FALSE;
   cur CURSOR FOR
    SELECT parent.name FROM FXS_TREE AS node, FXS_TREE AS parent
    WHERE node.lft>=parent.lft and node.lft<=parent.rgt AND node.id=nodeId
    ORDER BY parent.lft;
   curLive CURSOR FOR
    SELECT parent.name FROM FXS_TREE_LIVE AS node, FXS_TREE_LIVE AS parent
    WHERE node.lft>=parent.lft and node.lft<=parent.rgt AND node.id=nodeId
    ORDER BY parent.lft;

BEGIN
  EXCEPTION WHEN SQLSTATE '02000' THEN done = true;

  IF nodeId=1 THEN
    return '/';
  END IF;

  IF live THEN
    OPEN curLive;
  ELSE
    OPEN cur;
  END IF;

  name = '';
  result = '';
  WHILE NOT done LOOP
    IF live THEN
      FETCH curLive INTO name;
    ELSE
      FETCH cur INTO name;
    END IF;

    if NOT done THEN
      result = concat(result,"/");
      result = concat(result,name);
    END IF;
  END LOOP;

  result = substring(result,6);

  IF live THEN
      CLOSE curLive;
  ELSE
      CLOSE cur;
  END IF;

  return result;
END;
$$ LANGUAGE 'plpgsql';