Create OR REPLACE function getTemplates(nodeId INTEGER,live boolean)
returns text AS $$ -- deterministic reads sql data
DECLARE
-- template --> _template (keyword)
  result text default '';
  _template varchar(2000) default null;
  done BOOLEAN DEFAULT FALSE;
  cur CURSOR FOR
    SELECT parent.template FROM FXS_TREE AS node, FXS_TREE AS parent
    WHERE node.lft>=parent.lft and node.lft<=parent.rgt AND node.id=nodeId
    ORDER BY parent.lft desc;
  curLive CURSOR FOR
    SELECT parent.template FROM FXS_TREE_LIVE AS node, FXS_TREE_LIVE AS parent
    WHERE node.lft>=parent.lft and node.lft<=parent.rgt AND node.id=nodeId
    ORDER BY parent.lft desc;
BEGIN
  EXCEPTION WHEN SQLSTATE '02000' THEN done = true;

  IF live THEN
    OPEN curLive;
  ELSE
    OPEN cur;
  END IF;

  WHILE NOT done LOOP
    _template = null;
    IF live THEN
      FETCH curLive INTO _template;
    ELSE
      FETCH cur INTO _template;
    END IF;

    IF template IS NOT NULL THEN
      IF result!='' THEN
        result = concat(result,",");
      END IF;
      result = concat(result,_template);
    END IF;

  END LOOP;

  IF live THEN
      CLOSE curLive;
  ELSE
      CLOSE cur;
  END IF;

  return result;
END;
$$ LANGUAGE 'plpgsql';