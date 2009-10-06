-- deprecated and used for testing purposes only!
Create OR REPLACE function printTree(_lang char(2))
returns text AS $$ -- deterministic reads sql data
  DECLARE
	result text;
	tmp text;
	val text;
	ct int;
	nodeId int;
	done BOOLEAN DEFAULT FALSE;
	-- TODO: "node.name name" changed to "node.name nname"
	DECLARE cur CURSOR FOR
	  SELECT (COUNT(parent.id) - 1), node.name nname, node.id AS name
	  FROM FXS_TREE AS node, FXS_TREE AS parent
	  WHERE node.lft>parent.lft AND node.rgt<parent.rgt
	  GROUP BY node.id
	  ORDER BY node.lft;
BEGIN
  EXCEPTION WHEN SQLSTATE '02000' THEN done = true;
  OPEN cur;
  result = '';
  val = '';
  tmp = '';
  WHILE NOT done LOOP
    FETCH cur INTO ct,val,nodeId;
    if NOT done THEN
      result = concat(result,"\n");
      result = concat(result,repeat('-',ct));
      result = concat(result,nodeId);
      result = concat(result,":");
      result = concat(result,val);
      result = concat(result,'[level=');
      result = concat(result,ct);
      result = concat(result,']');
    END IF;
  END LOOP;
  CLOSE cur;
  return result;
END;
$$ LANGUAGE 'plpgsql';
