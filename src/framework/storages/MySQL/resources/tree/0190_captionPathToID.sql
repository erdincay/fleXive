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
drop function if exists tree_captionPathToID|
Create function tree_captionPathToID(_startNode INTEGER UNSIGNED,_path TEXT,_tprop INTEGER UNSIGNED,_lang INTEGER UNSIGNED,_live BOOLEAN)
returns INTEGER UNSIGNED deterministic reads sql data
BEGIN
  DECLARE _result INTEGER UNSIGNED default _startNode;
  DECLARE _current char(255) default '';
  DECLARE done BOOLEAN DEFAULT FALSE;
  DECLARE notfound BOOLEAN DEFAULT FALSE;
  DECLARE CONTINUE HANDLER FOR SQLSTATE '02000' SET notfound = TRUE;

  -- remove trailing '/'
  IF RIGHT(_path,1)='/' THEN
    set _path = substring(_path,1,CHAR_LENGTH(_path)-1);
  END IF;

  WHILE NOT done DO
    select substring(last,2),SUBSTRING(value,CHAR_LENGTH(last)+1)
    into _current,_path from(
    select input.value value,SUBSTRING_INDEX(input.value,'/',2) last from
      (select _path value from dual) input
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

  END WHILE;

END|



