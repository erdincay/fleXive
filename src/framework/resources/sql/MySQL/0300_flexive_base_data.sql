-- Optional data for a working base 

-- Test user "s"
INSERT INTO FXS_ACCOUNTS VALUES
(4,0,'s','ade6cce9baa10421c547578a2c3d32cf9a36fc24','dummy@dummy.com',null,
SYSDATE(),'3000-01-01 00:00:00','',
1,SYSDATE(),1,SYSDATE(),TRUE,TRUE,1,'s',TRUE,null,null);
INSERT INTO FXS_USERGROUPMEMBERS VALUES(4,1);

INSERT INTO FXS_ROLEMAPPING VALUES (4,3,1);
