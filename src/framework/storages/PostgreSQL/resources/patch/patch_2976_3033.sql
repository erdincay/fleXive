# Remove length limit for briefcase metadata entries

ALTER TABLE FXS_BRIEFCASE_DATA ALTER COLUMN METADATA TYPE TEXT;