-- Patch from v2562 -> v2641
-- Change: Mandator specific phrase management
-- Author: Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)

-- -------------------------
-- PHRASES
-- -------------------------
CREATE TABLE FX_PHRASE (
  ID BIGINT NOT NULL,
  MANDATOR BIGINT NOT NULL,
  PKEY VARCHAR(250) NOT NULL,
  PRIMARY KEY(ID, MANDATOR),
  FOREIGN KEY (MANDATOR) REFERENCES FXS_MANDATOR(ID)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT UNIQUE_PKEY UNIQUE(ID,MANDATOR,PKEY)
);
COMMENT ON TABLE FX_PHRASE IS 'Phrases';
CREATE INDEX FXI_PHRASE_KEY ON FX_PHRASE(MANDATOR, PKEY);

-- -------------------------
-- PHRASE VALUES
-- -------------------------
CREATE TABLE FX_PHRASE_VAL (
  ID BIGINT NOT NULL,
  MANDATOR BIGINT NOT NULL,
  LANG INTEGER NOT NULL,
  PVAL TEXT NOT NULL,
  TAG VARCHAR(250),
  PRIMARY KEY(ID,MANDATOR,LANG),
  FOREIGN KEY (ID,MANDATOR) REFERENCES FX_PHRASE(ID,MANDATOR)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  FOREIGN KEY (LANG) REFERENCES FXS_LANG(LANG_CODE)
  	ON DELETE RESTRICT ON UPDATE RESTRICT,
  FOREIGN KEY (MANDATOR) REFERENCES FXS_MANDATOR(ID)
    ON DELETE RESTRICT ON UPDATE RESTRICT
);
COMMENT ON TABLE FX_PHRASE_VAL IS 'Phrase values';

-- -------------------------
-- PHRASE TREE
-- -------------------------
CREATE TABLE FX_PHRASE_TREE (
  ID BIGINT NOT NULL,
  MANDATOR BIGINT NOT NULL,
  PARENTID BIGINT,
  PARENTMANDATOR BIGINT,
  PHRASEID BIGINT NOT NULL,
  PMANDATOR BIGINT NOT NULL,
  POS INTEGER NOT NULL DEFAULT 0,
  PRIMARY KEY(ID, MANDATOR),
  FOREIGN KEY (MANDATOR) REFERENCES FXS_MANDATOR(ID)
  	ON DELETE RESTRICT ON UPDATE RESTRICT,
  FOREIGN KEY (PARENTMANDATOR) REFERENCES FXS_MANDATOR(ID)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  FOREIGN KEY (PHRASEID, PMANDATOR) REFERENCES FX_PHRASE(ID, MANDATOR)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  FOREIGN KEY (PMANDATOR) REFERENCES FXS_MANDATOR(ID)
    ON DELETE RESTRICT ON UPDATE RESTRICT
);
COMMENT ON TABLE FX_PHRASE_TREE IS 'Phrase Tree';

-- -------------------------
-- PHRASE MAPPING
-- -------------------------
CREATE TABLE FX_PHRASE_MAP (
  MANDATOR BIGINT NOT NULL,
  NODEID BIGINT NOT NULL,
  NODEMANDATOR BIGINT NOT NULL,
  PHRASEID BIGINT NOT NULL,
  PMANDATOR BIGINT NOT NULL,
  POS INTEGER NOT NULL DEFAULT 0,
  PRIMARY KEY(MANDATOR, NODEID, NODEMANDATOR, PHRASEID, PMANDATOR),
  FOREIGN KEY (MANDATOR) REFERENCES FXS_MANDATOR(ID)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  FOREIGN KEY (NODEMANDATOR) REFERENCES FXS_MANDATOR(ID)
  	ON DELETE RESTRICT ON UPDATE RESTRICT,
  FOREIGN KEY (PMANDATOR) REFERENCES FXS_MANDATOR(ID)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  FOREIGN KEY (PHRASEID,PMANDATOR) REFERENCES FX_PHRASE(ID,MANDATOR)
    ON DELETE RESTRICT ON UPDATE RESTRICT
);
COMMENT ON TABLE FX_PHRASE_MAP IS 'Phrase Mapping';
CREATE INDEX FXI_PHRASE_MAP_KEY ON FX_PHRASE_MAP(PHRASEID, PMANDATOR);
CREATE INDEX FXI_PHRASE_MAP_POS ON FX_PHRASE_MAP(NODEMANDATOR, POS);
