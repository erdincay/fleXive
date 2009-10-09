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
CREATE OR REPLACE FUNCTION tree_getPosition(live BOOLEAN, nodeId BIGINT,parentId BIGINT)
RETURNS INT AS $$ -- DETERMINISTIC READS SQL DATA
DECLARE
  _count INTEGER DEFAULT 0;
  done BOOLEAN DEFAULT 0;
  found BOOLEAN DEFAULT FALSE;
  currentId BIGINT DEFAULT 0;
  curEdit CURSOR FOR SELECT ID FROM FXS_TREE WHERE PARENT=parentId ORDER BY LFT;
  curLive CURSOR FOR SELECT ID FROM FXS_TREE_LIVE WHERE PARENT=parentId ORDER BY LFT;
BEGIN
  IF NOT live THEN
    OPEN curEdit;
    WHILE NOT done LOOP
      FETCH curEdit INTO currentId;
      IF NOT FOUND THEN
        done = TRUE;
      END IF;
      IF NOT done THEN
        _count = _count + 1;
        IF currentId = nodeId THEN
          done = TRUE;
          found =  TRUE;
        END IF;
      END IF;
    END LOOP;
    CLOSE curEdit;
  ELSE
    OPEN curLive;
    WHILE NOT done LOOP
      FETCH curLive INTO currentId;
      IF NOT FOUND THEN
        done = TRUE;
      END IF;
      IF NOT done THEN
        _count = _count + 1;
        IF currentId = nodeId THEN
          done = TRUE;
          found = TRUE;
        END IF;
      END IF;
    END LOOP;
    CLOSE curLive;
  END IF;

  IF found THEN
    return _count - 1;
  ELSE
    return 0;
  END IF;
END;
$$ LANGUAGE 'plpgsql';
