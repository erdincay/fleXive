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
Create OR REPLACE function tree_captionPathToID(_startNode BIGINT,path TEXT,_tprop BIGINT,_lang BIGINT,_live BOOLEAN)
returns BIGINT AS $$ -- deterministic reads sql data
DECLARE
  _result BIGINT DEFAULT _startNode;
  _current CHAR(255) DEFAULT '';
  done BOOLEAN DEFAULT FALSE;
  notfound BOOLEAN DEFAULT FALSE;
  _path TEXT DEFAULT path;
BEGIN
  -- remove trailing '/'
  IF RIGHT(_path,1)='/' THEN
    _path = SUBSTRING(_path,1,CHAR_LENGTH(_path)-1);
  END IF;

  WHILE NOT done LOOP
    -- TODO: value --> _value + last --> _last (keyword)
    SELECT SUBSTRING(last,2),SUBSTRING(_value,CHAR_LENGTH(_last)+1)
    INTO _current,_path FROM(
    SELECT input.value _value,SUBSTRING_INDEX(input.value,'/',2) _last FROM
      (SELECT _path _value FROM dual) input
    ) parsed;

    IF _live THEN
      SELECT id INTO _result FROM FXS_TREE_LIVE node WHERE
          ifnull(ifnull(
           (SELECT f.FTEXT1024 FROM FX_CONTENT_DATA f WHERE f.tprop=_tprop AND lang IN (_lang,0) AND f.ISMAX_VER=TRUE AND f.id=node.ref LIMIT 1),
           (SELECT f.FTEXT1024 FROM FX_CONTENT_DATA f WHERE f.tprop=_tprop AND ismldef=TRUE AND f.ISMAX_VER=TRUE AND f.id=node.ref LIMIT 1)
          ),node.name) =_current
        and parent=_result;
    ELSE
      select id into _result from FXS_TREE node where
          ifnull(ifnull(
           (SELECT f.FTEXT1024 FROM FX_CONTENT_DATA f WHERE f.tprop=_tprop AND lang IN (_lang,0) AND f.ISMAX_VER=TRUE AND f.id=node.ref LIMIT 1),
           (SELECT f.FTEXT1024 FROM FX_CONTENT_DATA f WHERE f.tprop=_tprop AND ismldef=TRUE AND f.ISMAX_VER=TRUE AND f.id=node.ref LIMIT 1)
          ),node.name)=_current
        AND parent=_result;
    END IF;

    IF notfound THEN
      RETURN NULL;
    END IF;

    IF _path='' THEN
      RETURN _result;
    END IF;

  END LOOP;

END;
$$ LANGUAGE 'plpgsql';