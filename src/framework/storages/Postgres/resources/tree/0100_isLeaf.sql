-- Is the given node a leaf node?
Create OR REPLACE function tree_isLeaf(nodeId int)
returns int AS $$ -- deterministic reads sql data
BEGIN
  return (select count(*)=0 from FXS_TREE where parent=nodeId);
END;
$$ LANGUAGE 'plpgsql';