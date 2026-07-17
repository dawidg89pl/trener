-- Execute once only for a database created by an older version of this project.
ALTER TABLE user_settings
  ALTER COLUMN open_ai_key_encrypted TYPE TEXT USING CASE WHEN open_ai_key_encrypted IS NULL THEN NULL ELSE convert_from(lo_get(open_ai_key_encrypted), 'UTF8') END,
  ALTER COLUMN strava_client_id_encrypted TYPE TEXT USING CASE WHEN strava_client_id_encrypted IS NULL THEN NULL ELSE convert_from(lo_get(strava_client_id_encrypted), 'UTF8') END,
  ALTER COLUMN strava_client_secret_encrypted TYPE TEXT USING CASE WHEN strava_client_secret_encrypted IS NULL THEN NULL ELSE convert_from(lo_get(strava_client_secret_encrypted), 'UTF8') END;

ALTER TABLE strava_tokens
  ALTER COLUMN access_token_encrypted TYPE TEXT USING convert_from(lo_get(access_token_encrypted), 'UTF8'),
  ALTER COLUMN refresh_token_encrypted TYPE TEXT USING convert_from(lo_get(refresh_token_encrypted), 'UTF8');

ALTER TABLE generated_workouts
  ALTER COLUMN zwo_content TYPE TEXT USING CASE WHEN zwo_content IS NULL THEN NULL ELSE convert_from(lo_get(zwo_content), 'UTF8') END;
