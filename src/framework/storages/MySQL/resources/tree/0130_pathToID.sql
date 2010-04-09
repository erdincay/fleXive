-- Get the id of a tree path's leaf
drop function if exists tree_pathToID|
Create function tree_pathToID(_startNode INTEGER UNSIGNED,_path text,_live boolean)
returns INTEGER UNSIGNED DETERMINISTIC READS SQL DATA
BEGIN
  DECLARE _result INTEGER UNSIGNED default _startNode;
  DECLARE _current varchar(1024) default '';
  DECLARE done BOOLEAN DEFAULT FALSE;
  DECLARE notfound BOOLEAN DEFAULT FALSE;
  DECLARE CONTINUE HANDLER FOR SQLSTATE '02000' SET notfound = TRUE;

  IF RIGHT(_path,1)='/' THEN
    set _path = substring(_path,1,length(_path)-1);
  END IF;

  WHILE NOT done DO
    select substring(last,2),SUBSTRING(value,length(last)+1)
    into _current,_path from(
    select input.value value,SUBSTRING_INDEX(input.value,'/',2) last from
      (select _path value from dual) input
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

  END WHILE;

END|



