CREATE TABLE `tb_largedata` (
	`idx` BIGINT UNSIGNED NOT NULL COMMENT 'indexing for paging',
	`date` VARCHAR(14) NOT NULL COMMENT 'date',
	`col3` VARCHAR(14) NULL,
	`col4` VARCHAR(14) NULL,
	`col5` VARCHAR(14) NULL,
	`col6` VARCHAR(14) NULL,
	`col7` VARCHAR(14) NULL,
	`col8` VARCHAR(14) NULL,
	`col9` VARCHAR(14) NULL,
	`col10` VARCHAR(14) NULL,
	`col11` VARCHAR(14) NULL,
	`col12` VARCHAR(14) NULL,
	`col13` VARCHAR(14) NULL,
	`col14` VARCHAR(14) NULL,
	`col15` VARCHAR(14) NULL,
	`col16` VARCHAR(14) NULL,
	`col17` VARCHAR(14) NULL,
	`col18` VARCHAR(14) NULL,
	`col19` VARCHAR(14) NULL,
	`col20` VARCHAR(14) NULL
)
COMMENT='large data'
COLLATE='utf8_general_ci'
;

-- prepare_data 프로시져로 실행하여 샘플데이터 만들자...

INSERT INTO `tb_largedata` (`idx`, `date`, `col3`, `col4`, `col5`, `col6`, `col7`, `col8`, `col9`, `col10`, `col11`, `col12`, `col13`, `col14`, `col15`, `col16`, `col17`, `col18`, `col19`, `col20`) 
VALUES (0, '20181010230122', 'aaaa', 'bbb', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);

