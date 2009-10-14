-- Get a chain of id's for the given node (from the root node) like /1/4/42 for node 42
Create OR REPLACE function tree_idchain(nodeId BIGINT ,live boolean)
returns text AS $$ -- deterministic reads sql data
DECLARE
  _result text default '';
  _id BIGINT default null;
  done BOOLEAN DEFAULT FALSE;
  _nodeId BIGINT default nodeId;

BEGIN
  IF _nodeId=1 THEN
    return '/1';
  END IF;

  WHILE NOT done LOOP
    _id = null;

    IF live THEN
      select parent into _id from FXS_TREE_LIVE where id=_nodeId;
    ELSE
      select parent into _id from FXS_TREE where id=_nodeId;
    END IF;
    IF NOT FOUND THEN
      done = TRUE;
    END IF;
    IF _id IS NOT NULL THEN
--
      _result = '/'||_nodeId||_result;
    END IF;

    IF _id=1 THEN
      _result = '/'||'1'||_result;
    END IF;

    _nodeId = _id;

  END LOOP;

  return _result;
END;
$$ LANGUAGE 'plpgsql';