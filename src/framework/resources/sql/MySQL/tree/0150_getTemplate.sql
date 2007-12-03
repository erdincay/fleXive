drop function if exists getTemplate|
Create function getTemplate(nodeId INTEGER UNSIGNED,live boolean)
returns INTEGER deterministic reads sql data
BEGIN
  DECLARE template INTEGER default null;
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

    IF live THEN
      FETCH curLive INTO template;
    ELSE
      FETCH cur INTO template;
    END IF;

    IF template IS NOT NULL THEN
      set done =true;
    END IF;

  END WHILE;

  IF live THEN
      CLOSE curLive;
  ELSE
      CLOSE cur;
  END IF;

  return template;
END|



