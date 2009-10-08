--CREATE LANGUAGE plpgsql;
CREATE OR REPLACE FUNCTION TIMEMILLIS(TIMESTAMP WITH TIME ZONE) RETURNS BIGINT
    AS 'SELECT CAST(TRUNC(EXTRACT(EPOCH FROM $1)*1000) AS BIGINT);'
    LANGUAGE SQL;

-- see http://archives.postgresql.org/pgsql-admin/2003-08/msg00042.php
-- with modifications outlined in http://archives.postgresql.org/pgsql-admin/2003-08/msg00044.php
-- @START@
CREATE OR REPLACE FUNCTION make_concat_ws() RETURNS TEXT AS '
DECLARE
  v_args INT := 32;

v_first TEXT := ''CREATE OR REPLACE FUNCTION concat_ws(text,text,text) RETURNS text AS ''''SELECT CASE WHEN $1 IS NULL THEN NULL WHEN $3 IS NULL THEN $2 ELSE $2 || $1 || $3 END'''' LANGUAGE SQL IMMUTABLE STRICT'';

  v_part1 TEXT := ''CREATE OR REPLACE FUNCTION concat_ws(text,text'';

v_part2 TEXT := '') RETURNS text AS ''''SELECT concat_ws($1,concat_ws($1,$2'';

  v_part3 TEXT := '')'''' LANGUAGE SQL IMMUTABLE STRICT'';
  v_sql TEXT;
BEGIN
  EXECUTE v_first;
  FOR i IN 4 .. v_args LOOP
    v_sql := v_part1;
    FOR j IN 3 .. i LOOP
      v_sql := v_sql || '',text'';
    END LOOP;

    v_sql := v_sql || v_part2;

    FOR j IN 3 .. i - 1 LOOP
      v_sql := v_sql || '',$'' || j::text;
    END LOOP;
    v_sql := v_sql || ''),$'' || i::text;

    v_sql := v_sql || v_part3;
    EXECUTE v_sql;
  END LOOP;
  RETURN ''OK'';
END;
' language 'plpgsql';

-- @END@

SELECT make_concat_ws();

