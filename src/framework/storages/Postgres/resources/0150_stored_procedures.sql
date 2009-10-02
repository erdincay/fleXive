--CREATE LANGUAGE plpgsql;
CREATE OR REPLACE FUNCTION timemillis(timestamp with time zone) Returns double precision
    AS 'SELECT trunc(EXTRACT(EPOCH FROM $1)*1000);'
    LANGUAGE SQL;

