-- Get a chain of nodes with their caption from the root node plus additional information
-- Result format: /<node name>:<nodeId>:<refId>:<typeDefId>/...
Create OR REPLACE function tree_FTEXT1024_Chain(nodeId BIGINT,_lang BIGINT,_tprop BIGINT,_live boolean)
returns text AS $$-- CHARSET UTF8 deterministic reads sql data
DECLARE
  _result text default '';
  _id BIGINT default null;
  _tdef BIGINT default null;
  _ref BIGINT default null;
  _display text;
  _nodeName text;
  done BOOLEAN DEFAULT FALSE;
  _nodeId BIGINT default nodeId; -- TODO arguments are constant!
BEGIN
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
      select parent,ref,name,COALESCE((select tdef from FX_CONTENT where id=ref and ismax_ver=true limit 1),-1) AS tdef
      into _id,_ref,_nodeName,_tdef from FXS_TREE_LIVE where id=_nodeId;
    ELSE
      select parent,ref,name,COALESCE((select tdef from FX_CONTENT where id=ref and ismax_ver=true limit 1),-1) AS tdef
      into _id,_ref,_nodeName,_tdef from FXS_TREE where id=_nodeId;
    END IF;
    IF NOT FOUND THEN
      done = TRUE;
    END IF;

    -- --------------------------------------------------------------
    -- Retrieve display value from the content ----------------------
    -- --------------------------------------------------------------
    IF (_live) THEN
     select
     COALESCE(COALESCE(
	(select FTEXT1024 from FX_CONTENT_DATA where id=_ref and ISLIVE_VER=true and tprop=_tprop and lang=_lang LIMIT 1 OFFSET 0),
	(select FTEXT1024 from FX_CONTENT_DATA where id=_ref and ISLIVE_VER=true and tprop=_tprop and ISMLDEF LIMIT  1 OFFSET 0)
     ),_nodeName) into _display;
    ELSE
     select
     COALESCE(COALESCE(
	(select FTEXT1024 from FX_CONTENT_DATA where id=_ref and ISMAX_VER=true and tprop=_tprop and lang=_lang LIMIT  1 OFFSET 0),
	(select FTEXT1024 from FX_CONTENT_DATA where id=_ref and ISMAX_VER=true and tprop=_tprop and ISMLDEF LIMIT  1 OFFSET 0)
     ),_nodeName) into _display;
    END IF;
    IF NOT FOUND THEN
      done = TRUE;
    END IF;

     -- Handle 'null' values
     IF _display is null THEN
       _display = '<null>';
     END IF;
     -- '/' and ':' are reserved characters, so we replace them with a space
     _display = replace(_display,'/',' ');
     _display = replace(_display,':',' ');

    IF _id IS NOT NULL THEN
      _display = _display||':'||_nodeId||':'||_ref||':'||_tdef;
      _result = '/'||_display||_result;
    END IF;

    _nodeId = _id;

  END LOOP;

  return _result;
END;
$$ LANGUAGE 'plpgsql';
