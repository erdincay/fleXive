-- Is the given node a leaf node?
drop function if exists tree_isLeaf|
Create function tree_isLeaf(nodeId int)
returns int deterministic reads sql data
BEGIN
  return (select count(*)=0 from FXS_TREE where parent=nodeId);
END|
