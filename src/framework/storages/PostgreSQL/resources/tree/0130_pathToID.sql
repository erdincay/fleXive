-- Get the id of a tree path's leaf
CREATE OR REPLACE function tree_pathToID(_startNode BIGINT,path text,_live boolean)
RETURNS BIGINT AS $$ -- DETERMINISTIC READS SQL DATA
DECLARE
  _result BIGINT default _startNode;
  _current char(1024) default '';
  done BOOLEAN DEFAULT FALSE;
  notfound BOOLEAN DEFAULT FALSE;
  _path text;
BEGIN
  _path = path;

  IF substring(_path FROM length(_path)) = '/' THEN
    _path = substring(_path,1,length(_path)-1);
  END IF;
  -- remove leading '/'
  IF substring(_path,1,1)='/' THEN
    _path = substring(_path,2,length(_path));
  END IF;
  WHILE NOT done LOOP
    SELECT split_part(_path,'/',1) INTO _current;
    SELECT substring(_path, length(_current)+2) INTO _path;
    IF NOT FOUND THEN
      notfound = TRUE;
    END IF;

    IF _live THEN
      SELECT id INTO _result FROM FXS_TREE_LIVE WHERE name=_current AND parent=_result LIMIT 1;
    ELSE
      SELECT id INTO _result FROM FXS_TREE WHERE name=_current AND parent=_result LIMIT 1;
    END IF;
    IF NOT FOUND THEN
      notfound = TRUE;
    END IF;

    IF notfound THEN
      return null;
    END IF;

    if _path='' THEN
      return _result;
    END IF;

  END LOOP;

END;
$$ LANGUAGE 'plpgsql';