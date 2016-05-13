CREATE TABLE IF NOT EXISTS `CERTIFIED_QUIZ_RECORD` (
  `RESPONSE_ID` bigint NOT NULL,
  `USER_ID` bigint NOT NULL,
  `PASSED` bit(1) NOT NULL,
  `PASSED_ON` bigint NOT NULL,
  PRIMARY KEY (`RESPONSE_ID`),
  INDEX (USER_ID)
)