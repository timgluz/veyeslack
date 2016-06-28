CREATE TABLE IF NOT EXISTS notifications(
  id serial PRIMARY KEY,
  team_id varchar(255),
  n_tries integer DEFAULT 0,
  success boolean DEFAULT false NOT NULL,
  items jsonb,
  sent_at timestamp,
  created_at timestamp NOT NULL DEFAULT now(),
  updated_at timestamp NOT NULL DEFAULT now()
);
--;;
CREATE INDEX idx_team_id_on_notifications ON notifications(team_id)


