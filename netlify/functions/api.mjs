import { neon } from '@neondatabase/serverless';

let _sql;
const sql = (...args) => { if (!_sql) _sql = neon(process.env.DATABASE_URL); return _sql(...args); };
const ADMIN = process.env.ADMIN_TOKEN || 'change-me';
const RAW_HANDLE = process.env.TIKTOK_HANDLE || 'avgpiano';
const HANDLE = RAW_HANDLE.replace(/^.*tiktok\.com\/@?/, '').replace(/^@/, '').replace(/\/.*$/, '');
const BIO = process.env.TIKTOK_BIO || 'I play your song requests on piano';
const AVATAR = process.env.TIKTOK_AVATAR || '';

const cors = {
  'Content-Type': 'application/json',
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'Content-Type, X-Admin-Token',
  'Access-Control-Allow-Methods': 'GET, POST, DELETE, OPTIONS',
};

const json = (s, b) => ({ statusCode: s, headers: cors, body: b != null ? JSON.stringify(b) : '' });

export async function handler(event) {
  if (event.httpMethod === 'OPTIONS') return { statusCode: 204, headers: cors, body: '' };

  const path = event.path.replace(/^\/api/, '');
  const method = event.httpMethod;
  const adminOk = (event.headers['x-admin-token'] || '') === ADMIN;

  try {
    // ── Profile ──
    if (method === 'GET' && path === '/profile') {
      return json(200, { handle: HANDLE, bio: BIO, avatarUrl: AVATAR, tiktokLink: `https://www.tiktok.com/@${HANDLE}` });
    }

    // ── Song search (iTunes proxy) ──
    if (method === 'GET' && path === '/songs/search') {
      const q = (event.queryStringParameters?.q || '').trim();
      if (q.length < 2) return json(200, []);
      try {
        const res = await fetch(
          `https://itunes.apple.com/search?term=${encodeURIComponent(q)}&media=music&entity=song&limit=8`
        );
        const data = await res.json();
        return json(200, (data.results || []).map(r => ({
          trackName: r.trackName || '', artistName: r.artistName || '', artworkUrl: r.artworkUrl100 || ''
        })));
      } catch { return json(200, []); }
    }

    // ── Get queue ──
    if (method === 'GET' && path === '/requests') {
      const rows = await sql`SELECT * FROM song_request WHERE status='QUEUED' ORDER BY created_at ASC`;
      return json(200, rows.map((r, i) => ({
        id: r.id, position: i + 1,
        trackName: r.track_name, artistName: r.artist_name, artworkUrl: r.artwork_url,
        requesterName: (adminOk || r.name_public) ? r.requester_name : null,
        namePublic: r.name_public, createdAt: r.created_at
      })));
    }

    // ── Played history ──
    if (method === 'GET' && path === '/requests/history') {
      const rows = await sql`SELECT * FROM song_request WHERE status='PLAYED' ORDER BY created_at DESC`;
      return json(200, rows.map(r => ({
        id: r.id, position: 0,
        trackName: r.track_name, artistName: r.artist_name, artworkUrl: r.artwork_url,
        requesterName: r.name_public ? r.requester_name : null,
        namePublic: r.name_public, createdAt: r.created_at
      })));
    }

    // ── Stats (admin) ──
    if (method === 'GET' && path === '/requests/stats') {
      if (!adminOk) return json(403, { error: 'Invalid token' });
      const [s] = await sql`
        SELECT COUNT(*) FILTER (WHERE status='QUEUED') AS queued,
               COUNT(*) FILTER (WHERE status='PLAYED') AS played,
               COUNT(*) AS total
        FROM song_request`;
      return json(200, { queued: +s.queued, played: +s.played, total: +s.total });
    }

    // ── Submit request ──
    if (method === 'POST' && path === '/requests') {
      const b = JSON.parse(event.body || '{}');
      const name = (b.requesterName || '').trim();
      const track = (b.trackName || '').trim();
      const artist = (b.artistName || '').trim();
      const art = (b.artworkUrl || '').trim();
      const pub = !!b.namePublic;

      if (!name || !track) return json(400, { error: 'Name and song are required' });
      if (name.length > 50) return json(400, { error: 'Name too long (max 50)' });
      if (track.length > 200) return json(400, { error: 'Song name too long (max 200)' });

      // Rate limit: max 5 submissions per name per hour
      const [rc] = await sql`
        SELECT COUNT(*) AS cnt FROM song_request
        WHERE LOWER(requester_name)=LOWER(${name}) AND created_at > NOW()-INTERVAL '1 hour'`;
      if (+rc.cnt >= 5) return json(429, { error: 'Too many requests. Try again later.' });

      // Duplicate check
      const dup = await sql`
        SELECT 1 FROM song_request
        WHERE LOWER(requester_name)=LOWER(${name})
          AND LOWER(track_name)=LOWER(${track})
          AND LOWER(COALESCE(artist_name,''))=LOWER(${artist})
          AND status='QUEUED' LIMIT 1`;
      if (dup.length) return json(409, { error: 'You already requested this song' });

      const [row] = await sql`
        INSERT INTO song_request (requester_name,name_public,track_name,artist_name,artwork_url,status,created_at)
        VALUES (${name},${pub},${track},${artist},${art},'QUEUED',NOW()) RETURNING id`;

      const queued = await sql`SELECT id FROM song_request WHERE status='QUEUED' ORDER BY created_at ASC`;
      const position = queued.findIndex(r => r.id === row.id) + 1;
      return json(201, { id: row.id, position });
    }

    // ── Mark played (admin) ──
    const playM = path.match(/^\/requests\/(\d+)\/played$/);
    if (method === 'POST' && playM) {
      if (!adminOk) return json(403, { error: 'Invalid token' });
      const res = await sql`UPDATE song_request SET status='PLAYED' WHERE id=${+playM[1]} AND status='QUEUED' RETURNING id`;
      return res.length ? json(200, { status: 'played' }) : json(404, { error: 'Not found' });
    }

    // ── Delete (admin) ──
    const delM = path.match(/^\/requests\/(\d+)$/);
    if (method === 'DELETE' && delM) {
      if (!adminOk) return json(403, { error: 'Invalid token' });
      const res = await sql`DELETE FROM song_request WHERE id=${+delM[1]} RETURNING id`;
      return res.length ? json(204, null) : json(404, { error: 'Not found' });
    }

    return json(404, { error: 'Not found' });
  } catch (err) {
    console.error(err);
    return json(500, { error: 'Server error' });
  }
}
