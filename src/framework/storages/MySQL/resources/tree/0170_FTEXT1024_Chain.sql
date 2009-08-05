-- Get a chain of nodes with their caption from the root node plus additional information
-- Result format: /<node name>:<nodeId>:<refId>:<typeDefId>/...
drop function if exists tree_FTEXT1024_Chain|
Create function tree_FTEXT1024_Chain(_nodeId INTEGER UNSIGNED,_lang INTEGER,_tprop INTEGER UNSIGNED,_live boolean)
returns text CHARSET UTF8 deterministic reads sql data
BEGIN
  DECLARE _result text default '';
  DECLARE _id INTEGER UNSIGNED default null;
  DECLARE _tdef INTEGER default null;
  DECLARE _ref INTEGER UNSIGNED default null;
  DECLARE _display text;
  DECLARE _nodeName text;
  DECLARE done BOOLEAN DEFAULT FALSE;
  DECLARE CONTINUE HANDLER FOR SQLSTATE '02000' SET done = TRUE;

  IF _nodeId=1 THEN
    return '/';
  END IF;

  WHILE NOT done DO

    SET _id = null;
    SET _ref = null;
    SET _display = null;


    -- --------------------------------------------------------------
    -- READ the node id and ref values from the tree ----------------
    -- --------------------------------------------------------------
    IF _live THEN
      select parent,ref,name,ifnull((select tdef from FX_CONTENT where id=ref and ismax_ver=true limit 1),-1) tdef
      into _id,_ref,_nodeName,_tdef from FXS_TREE_LIVE where id=_nodeId;
    ELSE
      select parent,ref,name,ifnull((select tdef from FX_CONTENT where id=ref and ismax_ver=true limit 1),-1) tdef
      into _id,_ref,_nodeName,_tdef from FXS_TREE where id=_nodeId;
    END IF;

    -- --------------------------------------------------------------
    -- Retrieve display value from the content ----------------------
    -- --------------------------------------------------------------
    IF (_live) THEN
     select
     ifnull(ifnull(
	(select FTEXT1024 from FX_CONTENT_DATA where id=_ref and ISLIVE_VER=true and tprop=_tprop and lang=_lang LIMIT 0,1),
	(select FTEXT1024 from FX_CONTENT_DATA where id=_ref and ISLIVE_VER=true and tprop=_tprop and ISMLDEF LIMIT 0,1)
     ),_nodeName) into _display;
    ELSE
     select
     ifnull(ifnull(
	(select FTEXT1024 from FX_CONTENT_DATA where id=_ref and ISMAX_VER=true and tprop=_tprop and lang=_lang LIMIT 0,1),
	(select FTEXT1024 from FX_CONTENT_DATA where id=_ref and ISMAX_VER=true and tprop=_tprop and ISMLDEF LIMIT 0,1)
     ),_nodeName) into _display;
    END IF;

     -- Handle 'null' values
     IF _display is null THEN
       set _display = '<null>';
     END IF;
     -- '/' and ':' are reserved characters, so we replace them with a space
     set _display = replace(_display,'/',' ');
     set _display = replace(_display,':',' ');


    IF _id IS NOT NULL THEN
      set _display = concat(_display,':');
      set _display = concat(_display,_nodeId);
      set _display = concat(_display,':');
      set _display = concat(_display,_ref);
      set _display = concat(_display,':');
      set _display = concat(_display,_tdef);      
      set _result = concat(_display,_result);
      set _result = concat("/",_result);
    END IF;

    set _nodeId = _id;

  END WHILE;

  return _result;
END|
