/**
 * function: tree_captionPathToID
 * description: Find the node-Id from a Caption-Path.
 *              The path is being processed from left to right to match the name
 * author: Gregor Schober
 * author: Markus Plesser
 * date: 20070917
 *
 * parameters:
 *   startNode node id where the path starts
 *   path      the given path
 *   lang      language id
 *   live     use the live or edit table?
 * returns:
 *   found nodeId or NULL if no node was found
 */
Create OR REPLACE function tree_captionPathToID(_startNode INTEGER,path TEXT,_tprop INTEGER,_lang INTEGER,_live BOOLEAN)
returns INTEGER AS $$ -- deterministic reads sql data
DECLARE
  _result INTEGER default _startNode;
  _current char(255) default '';
  done BOOLEAN DEFAULT FALSE;
  notfound BOOLEAN DEFAULT FALSE;
  _path TEXT default path;
BEGIN
  EXCEPTION WHEN SQLSTATE '02000' THEN notfound = true;

  -- remove trailing '/'
  IF RIGHT(_path,1)='/' THEN
    _path = substring(_path,1,CHAR_LENGTH(_path)-1);
  END IF;

  WHILE NOT done LOOP
    -- TODO: value --> _value + last --> _last (keyword)
    select substring(last,2),SUBSTRING(_value,CHAR_LENGTH(_last)+1)
    into _current,_path from(
    select input.value _value,SUBSTRING_INDEX(input.value,'/',2) _last from
      (select _path _value from dual) input
    ) parsed;

    IF _live THEN
      select id into _result from FXS_TREE_LIVE node where
          ifnull(ifnull(
           (select f.FTEXT1024 from FX_CONTENT_DATA f where f.tprop=_tprop and lang in (_lang,0) and f.ISMAX_VER=true and f.id=node.ref limit 1),
           (select f.FTEXT1024 from FX_CONTENT_DATA f where f.tprop=_tprop and ismldef=true and f.ISMAX_VER=true and f.id=node.ref limit 1)
          ),node.name) =_current
        and parent=_result;
    ELSE
      select id into _result from FXS_TREE node where
          ifnull(ifnull(
           (select f.FTEXT1024 from FX_CONTENT_DATA f where f.tprop=_tprop and lang in (_lang,0) and f.ISMAX_VER=true and f.id=node.ref limit 1),
           (select f.FTEXT1024 from FX_CONTENT_DATA f where f.tprop=_tprop and ismldef=true and f.ISMAX_VER=true and f.id=node.ref limit 1)
          ),node.name)=_current
        and parent=_result;
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