CREATE TABLE IF NOT EXISTS api_keys(
  id serial primary key,
  team_id varchar(255) NOT NULL,
  user_id varchar(255) NOT NULL,
  api_key varchar(255) NOT NULL,
  created_at timestamp NOT NULL DEFAULT now(),
  updated_at timestamp NOT NULL DEFAULT now()
);
--;;
CREATE UNIQUE INDEX idx_unique_team_user_api_keys ON api_keys(team_id, user_id);

--;;
CREATE INDEX idx_team_id_api_keys ON api_keys(team_id);
