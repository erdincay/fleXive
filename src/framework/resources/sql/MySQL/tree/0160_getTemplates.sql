drop function if exists getTemplates|
Create function getTemplates(nodeId INTEGER UNSIGNED,live boolean)
returns text deterministic reads sql data
BEGIN
  DECLARE result text default '';
  DECLARE template varchar(2000) default null;
  DECLARE done BOOLEAN DEFAULT FALSE;
  DECLARE cur CURSOR FOR
    SELECT parent.template FROM FXS_TREE AS node, FXS_TREE AS parent
    WHERE node.lft>=parent.lft and node.lft<=parent.rgt AND node.id=nodeId
    ORDER BY parent.lft desc;
  DECLARE curLive CURSOR FOR
    SELECT parent.template FROM FXS_TREE_LIVE AS node, FXS_TREE_LIVE AS parent
    WHERE node.lft>=parent.lft and node.lft<=parent.rgt AND node.id=nodeId
    ORDER BY parent.lft desc;
  DECLARE CONTINUE HANDLER FOR SQLSTATE '02000' SET done = TRUE;

  IF live THEN
    OPEN curLive;
  ELSE
    OPEN cur;
  END IF;

  WHILE NOT done DO
    set template = null;
    IF live THEN
      FETCH curLive INTO template;
    ELSE
      FETCH cur INTO template;
    END IF;

    IF template IS NOT NULL THEN
      IF result!='' THEN
        set result = concat(result,",");
      END IF;
      set result = concat(result,template);
    END IF;

  END WHILE;

  IF live THEN
      CLOSE curLive;
  ELSE
      CLOSE cur;
  END IF;

  return result;
END|



