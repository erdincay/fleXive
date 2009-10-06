-- Get a chain of nodes with their caption from the root node plus additional information
-- Result format: /<node name>:<nodeId>:<refId>:<typeDefId>/...
Create OR REPLACE function tree_FTEXT1024_Chain(nodeId INTEGER,_lang INTEGER,_tprop INTEGER,_live boolean)
returns text AS $$-- CHARSET UTF8 deterministic reads sql data
DECLARE
  _result text default '';
  _id INTEGER default null;
  _tdef INTEGER default null;
  _ref INTEGER default null;
  _display text;
  _nodeName text;
  done BOOLEAN DEFAULT FALSE;
  _nodeId INTEGER default nodeId; -- TODO arguments are constant!
BEGIN
  EXCEPTION WHEN SQLSTATE '02000' THEN done = true;

  IF _nodeId=1 THEN
    return '/';
  END IF;

  WHILE NOT done LOOP
    _id = null;
    _ref = null;
    _display = null;


    -- --------------------------------------------------------------
    -- READ the node id and ref values from the tree ----------------
    -- --------------------------------------------------------------
    IF _live THEN
    -- TODO LIMIT x, y --> LIMIT y OFFSET x
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
	(select FTEXT1024 from FX_CONTENT_DATA where id=_ref and ISLIVE_VER=true and tprop=_tprop and lang=_lang LIMIT 1 OFFSET 0),
	(select FTEXT1024 from FX_CONTENT_DATA where id=_ref and ISLIVE_VER=true and tprop=_tprop and ISMLDEF LIMIT  1 OFFSET 0)
     ),_nodeName) into _display;
    ELSE
     select
     ifnull(ifnull(
	(select FTEXT1024 from FX_CONTENT_DATA where id=_ref and ISMAX_VER=true and tprop=_tprop and lang=_lang LIMIT  1 OFFSET 0),
	(select FTEXT1024 from FX_CONTENT_DATA where id=_ref and ISMAX_VER=true and tprop=_tprop and ISMLDEF LIMIT  1 OFFSET 0)
     ),_nodeName) into _display;
    END IF;

     -- Handle 'null' values
     IF _display is null THEN
       _display = '<null>';
     END IF;
     -- '/' and ':' are reserved characters, so we replace them with a space
     _display = replace(_display,'/',' ');
     _display = replace(_display,':',' ');


    IF _id IS NOT NULL THEN
      _display = concat(_display,':');
      _display = concat(_display,_nodeId);
      _display = concat(_display,':');
      _display = concat(_display,_ref);
      _display = concat(_display,':');
      _display = concat(_display,_tdef);
      _result = concat(_display,_result);
      _result = concat("/",_result);
    END IF;

    _nodeId = _id;

  END LOOP;

  return _result;
END;
$$ LANGUAGE 'plpgsql';
