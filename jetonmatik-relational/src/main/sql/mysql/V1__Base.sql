create table `CLIENT` (`ID` VARCHAR(254) NOT NULL PRIMARY KEY,`SECRET_HASH` VARCHAR(254) NOT NULL,`SCOPE` VARCHAR(254) NOT NULL,`TOKEN_TTL` BIGINT NOT NULL,`LAST_MODIFIED` TIMESTAMP NOT NULL,`VERSION` BIGINT NOT NULL);