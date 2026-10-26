CREATE TABLE IF NOT EXISTS song_request (
  id SERIAL PRIMARY KEY,
  requester_name VARCHAR(50) NOT NULL,
  name_public BOOLEAN NOT NULL DEFAULT false,
  track_name VARCHAR(200) NOT NULL,
  artist_name VARCHAR(200),
  artwork_url VARCHAR(500),
  status VARCHAR(10) NOT NULL DEFAULT 'QUEUED',
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
