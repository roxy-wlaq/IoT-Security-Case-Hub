ALTER TABLE casehub.generation_conditions
    DROP CONSTRAINT chk_generation_conditions_operator;
ALTER TABLE casehub.generation_conditions
    ADD CONSTRAINT chk_generation_conditions_operator
    CHECK (operator IN ('EQ_YES', 'EQ_NO', 'EQ_UNKNOWN', 'NE_NO', 'NE_YES', 'PRESENT', 'ANY'));
