CREATE TABLE IF NOT EXISTS `USER_GROUP` (
  `ID` bigint(20) NOT NULL,
  `IS_INDIVIDUAL` boolean NOT NULL,
  `CREATED_ON` bigint(20) NOT NULL,
  PRIMARY KEY (`ID`, `IS_INDIVIDUAL`)
)