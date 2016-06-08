CREATE TABLE IF NOT EXISTS auth_tokens(
  id serial primary key,
  access_token varchar(255) not null,
  scope text,
  team_id varchar(255),
  team_name text not null,
  url text,
  channel varchar(255),
  configuration_url text,
  bot_user_id varchar(255),
  bot_access_token varchar(255)
);
--;;
CREATE INDEX idx_team_id ON auth_tokens(team_id);
--;;
CREATE INDEX idx_uniq_team_token ON auth_tokens(team_id, access_token);
