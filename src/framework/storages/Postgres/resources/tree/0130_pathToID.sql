-- Get the id of a tree path's leaf
Create OR REPLACE function tree_pathToID(_startNode INTEGER,path text,_live boolean)
returns INTEGER AS $$ -- DETERMINISTIC READS SQL DATA
DECLARE
  _result INTEGER default _startNode;
  _current char(255) default '';
  done BOOLEAN DEFAULT FALSE;
  notfound BOOLEAN DEFAULT FALSE;
  _path text;
BEGIN
  EXCEPTION WHEN SQLSTATE '02000' THEN notfound = true;

  _path = path;

  IF RIGHT(_path,1)='/' THEN
    _path = substring(_path,1,length(_path)-1);
  END IF;

-- TODO value --> _value + last --> _last (keywords)
  WHILE NOT done LOOP
    select substring(last,2),SUBSTRING(_value,length(_last)+1)
    into _current,_path from(
    select input.value as _value,SUBSTRING_INDEX(input.value,'/',2) _last from
      (select _path _value from dual) input
    ) parsed;


    IF _live THEN
      select id into _result from FXS_TREE_LIVE where name=_current and parent=_result;
    ELSE
      select id into _result from FXS_TREE where name=_current and parent=_result;
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