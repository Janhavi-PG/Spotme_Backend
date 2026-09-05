CREATE TABLE travel_claim (

                              travel_id UUID PRIMARY KEY
                                  DEFAULT gen_random_uuid(),

                              user_id UUID NOT NULL,

                              branch_id UUID NOT NULL,

                              travel_date DATE NOT NULL,

                              status VARCHAR(50) NOT NULL
                                  DEFAULT 'DRAFT',

                              activities_json JSONB NOT NULL
                                  DEFAULT '[]'::jsonb,

                              tagged_total_km DECIMAL(10,2),

                              total_km DECIMAL(10,2) NOT NULL
                                  DEFAULT 0,

                              total_amount DECIMAL(12,2) NOT NULL
                                  DEFAULT 0,

                              activity_count INT NOT NULL
                                  DEFAULT 0,

                              rate_per_km DECIMAL(10,2),

                              submitted_at TIMESTAMP,

                              claimed_at TIMESTAMP,

                              created_at TIMESTAMP NOT NULL
                                  DEFAULT NOW(),

                              updated_at TIMESTAMP NOT NULL
                                  DEFAULT NOW(),

                              CONSTRAINT fk_travel_claim_user
                                  FOREIGN KEY (user_id)
                                      REFERENCES users(id),

                              CONSTRAINT uq_travel_claim_user_date
                                  UNIQUE (user_id, travel_date)
);

