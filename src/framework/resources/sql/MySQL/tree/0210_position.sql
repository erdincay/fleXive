/**
 * function: tree_getPosition
 * description: get the position of a node that is a child node of parentId relative to all children of
 *              parentId starting at 1
 * author: Markus Plesser
 * date: 20070914
 *
 * parameters:
 *   live     use the live or edit table?
 *   nodeId   id of the node to get the position fort
 *   parentId id of the parent node (no checks are performed if nodeId is actually a child of parentId!)
 * returns:
 *   position relative to all children of the parent node, starting at 1 or NULL if the node is no child of parent
 *
 */
DROP FUNCTION IF EXISTS tree_getPosition |

CREATE FUNCTION tree_getPosition (live BOOLEAN, nodeId INTEGER UNSIGNED,parentId INTEGER UNSIGNED) RETURNS INT DETERMINISTIC READS SQL DATA
BEGIN
  DECLARE _count INTEGER UNSIGNED default 0;
  DECLARE done INT DEFAULT 0;
  DECLARE found BOOLEAN DEFAULT false;
  DECLARE currentId INT DEFAULT 0;
  DECLARE curEdit CURSOR FOR SELECT ID FROM FXS_TREE WHERE PARENT=parentId ORDER BY LFT;
  DECLARE curLive CURSOR FOR SELECT ID FROM FXS_TREE_LIVE WHERE PARENT=parentId ORDER BY LFT;
  DECLARE CONTINUE HANDLER FOR SQLSTATE '02000' SET done = 1;

  IF NOT live THEN
    OPEN curEdit;
    REPEAT
      FETCH curEdit INTO currentId;
      IF NOT done THEN
        SET _count = _count + 1;
        IF currentId = nodeId THEN
          SET done = 1;
          SET found =  true;
        END If;
      END IF;
    UNTIL done END REPEAT;
    CLOSE curEdit;
  ELSE
    OPEN curLive;
    REPEAT
      FETCH curLive INTO currentId;
      IF NOT done THEN
        SET _count = _count + 1;
        IF currentId = nodeId THEN
          SET done = 1;
          SET found =  true;
        END If;
      END IF;
    UNTIL done END REPEAT;
    CLOSE curLive;
  END IF;

  IF found THEN
    return _count - 1;
  ELSE
    return null;
  END IF;

END |
