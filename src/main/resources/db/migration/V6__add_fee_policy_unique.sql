ALTER TABLE fee_policies
    ADD CONSTRAINT uq_fee_policies_effective_from UNIQUE (effective_from);
