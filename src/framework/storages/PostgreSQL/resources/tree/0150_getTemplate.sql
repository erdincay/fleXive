Create OR REPLACE function getTemplate(nodeId BIGINT, live boolean)
returns INTEGER AS $$ --deterministic reads sql data
DECLARE
-- TODO teplate --> _template (keyword)
  _template BIGINT default null;
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
  IF live THEN
    OPEN curLive;
  ELSE
    OPEN cur;
  END IF;

  WHILE NOT done LOOP

    IF live THEN
      FETCH curLive INTO _template;
    ELSE
      FETCH cur INTO _template;
    END IF;
    IF NOT FOUND THEN
      done = TRUE;
    END IF;

    IF template IS NOT NULL THEN
      done =true;
    END IF;

  END LOOP;

  IF live THEN
      CLOSE curLive;
  ELSE
      CLOSE cur;
  END IF;

  return _template;
END;
$$ LANGUAGE 'plpgsql';