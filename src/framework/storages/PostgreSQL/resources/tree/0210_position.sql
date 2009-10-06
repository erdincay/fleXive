/**
 * function: tree_getPosition
 * description: get the position of a node that is a child node of parentId relative to all children of
 *              parentId starting at 1
 * author: Markus Plesser
 * date: 20070914
 *
 * parameters:
 *   live     use the live or edit table?
 *   nodeId   id of the node to get the position for
 *   parentId id of the parent node (no checks are performed if nodeId is actually a child of parentId!)
 * returns:
 *   position relative to all children of the parent node, starting at 1 or NULL if the node is no child of parent
 *
 */
CREATE OR REPLACE FUNCTION tree_getPosition (live BOOLEAN, nodeId INTEGER,parentId INTEGER)
RETURNS INT AS $$ -- DETERMINISTIC READS SQL DATA
DECLARE
  _count INTEGER default 0;
  done BOOLEAN DEFAULT 0;
  found BOOLEAN DEFAULT false;
  currentId INT DEFAULT 0;
  curEdit CURSOR FOR SELECT ID FROM FXS_TREE WHERE PARENT=parentId ORDER BY LFT;
  curLive CURSOR FOR SELECT ID FROM FXS_TREE_LIVE WHERE PARENT=parentId ORDER BY LFT;
BEGIN
  EXCEPTION WHEN SQLSTATE '02000' THEN done = true;

  IF NOT live THEN
    OPEN curEdit;
--    REPEAT
   WHILE NOT done LOOP
      FETCH curEdit INTO currentId;
      IF NOT done THEN
        _count = _count + 1;
        IF currentId = nodeId THEN
          done = true;
          found =  true;
        END If;
      END IF;
--    UNTIL done END REPEAT;
    END LOOP;
    CLOSE curEdit;
  ELSE
    OPEN curLive;
--    REPEAT
    WHILE NOT done LOOP
      FETCH curLive INTO currentId;
      IF NOT done THEN
        _count = _count + 1;
        IF currentId = nodeId THEN
          done = true;
          found =  true;
        END If;
      END IF;
--    UNTIL done END REPEAT;
    END LOOP;
    CLOSE curLive;
  END IF;

  IF found THEN
    return _count - 1;
  ELSE
    return null;
  END IF;

END;
$$ LANGUAGE 'plpgsql';
