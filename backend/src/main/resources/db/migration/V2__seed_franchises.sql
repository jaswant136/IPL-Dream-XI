-- Current IPL franchises. Only franchise identity is seeded here.
-- Roster (player_seasons) data is NEVER seeded in a migration: it is imported through the validated
-- CSV importer so that franchise/season associations are always checked.
INSERT INTO teams (id, code, name, active) VALUES ('b1a7c1d0-0001-4000-8000-000000000001', 'CSK',  'Chennai Super Kings',        TRUE);
INSERT INTO teams (id, code, name, active) VALUES ('b1a7c1d0-0001-4000-8000-000000000002', 'MI',   'Mumbai Indians',             TRUE);
INSERT INTO teams (id, code, name, active) VALUES ('b1a7c1d0-0001-4000-8000-000000000003', 'RCB',  'Royal Challengers Bengaluru', TRUE);
INSERT INTO teams (id, code, name, active) VALUES ('b1a7c1d0-0001-4000-8000-000000000004', 'KKR',  'Kolkata Knight Riders',      TRUE);
INSERT INTO teams (id, code, name, active) VALUES ('b1a7c1d0-0001-4000-8000-000000000005', 'DC',   'Delhi Capitals',             TRUE);
INSERT INTO teams (id, code, name, active) VALUES ('b1a7c1d0-0001-4000-8000-000000000006', 'SRH',  'Sunrisers Hyderabad',        TRUE);
INSERT INTO teams (id, code, name, active) VALUES ('b1a7c1d0-0001-4000-8000-000000000007', 'PBKS', 'Punjab Kings',               TRUE);
INSERT INTO teams (id, code, name, active) VALUES ('b1a7c1d0-0001-4000-8000-000000000008', 'RR',   'Rajasthan Royals',           TRUE);
INSERT INTO teams (id, code, name, active) VALUES ('b1a7c1d0-0001-4000-8000-000000000009', 'GT',   'Gujarat Titans',             TRUE);
INSERT INTO teams (id, code, name, active) VALUES ('b1a7c1d0-0001-4000-8000-000000000010', 'LSG',  'Lucknow Super Giants',       TRUE);
