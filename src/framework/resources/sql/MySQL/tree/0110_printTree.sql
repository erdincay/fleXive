-- deprecated and used for testing purposes only!
drop function if exists printTree|
Create function printTree(_lang char(2))
returns text deterministic reads sql data
BEGIN
  DECLARE result text;
  DECLARE tmp text;
  DECLARE val text;
  DECLARE ct int;
  DECLARE nodeId int;
  DECLARE done BOOLEAN DEFAULT FALSE;
  DECLARE cur CURSOR FOR SELECT (COUNT(parent.id) - 1),
    node.name name,
    node.id AS name
	FROM FXS_TREE AS node,
	FXS_TREE AS parent
	WHERE node.lft>parent.lft AND node.rgt<parent.rgt
	GROUP BY node.id
	ORDER BY node.lft;
  DECLARE CONTINUE HANDLER FOR SQLSTATE '02000' SET done = TRUE;
  OPEN cur;
  set result = '';
  set val = '';
  set tmp = '';
  WHILE NOT done DO
    FETCH cur INTO ct,val,nodeId;
    if NOT done THEN
      set result = concat(result,'\n');
      set result = concat(result,repeat('-',ct));
      set result = concat(result,nodeId);
      set result = concat(result,":");
      set result = concat(result,val);
      set result = concat(result,'[level=');
      set result = concat(result,ct);
      set result = concat(result,']');
    END IF;
  END WHILE;
  CLOSE cur;
  return result;
END|
