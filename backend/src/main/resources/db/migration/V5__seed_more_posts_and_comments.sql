
-- Additional users (password hash reused from V3's seeded users so login works with the same known password)
INSERT INTO users (id, email, name, password, role, created_at, updated_at) VALUES
  (3, 'alice@amalitech.com', 'Alice Mensah', '$2b$10$Fmxs/wd/psAKCLtmxxeDW.gNEeANTefNsT1WvuOLYVCj0BJLXvfoi', 'USER', NOW(), NOW()),
  (4, 'brian@amalitech.com', 'Brian Osei',   '$2b$10$Fmxs/wd/psAKCLtmxxeDW.gNEeANTefNsT1WvuOLYVCj0BJLXvfoi', 'USER', NOW(), NOW()),
  (5, 'clara@amalitech.com', 'Clara Adjei',  '$2b$10$Fmxs/wd/psAKCLtmxxeDW.gNEeANTefNsT1WvuOLYVCj0BJLXvfoi', 'USER', NOW(), NOW());

-- Additional posts spread across all four categories and authored by different users
INSERT INTO posts (id, title, slug, content, category_id, author_id, created_at, updated_at) VALUES
  (3,  'Quarterly All-Hands Recap',       'quarterly-all-hands-recap',       'A summary of the highlights and key decisions from this quarter''s all-hands meeting.', 1, 1, NOW(), NOW()),
  (4,  'New Office Wing Now Open',         'new-office-wing-now-open',         'The east wing is now open with new desks, meeting rooms, and a larger break area.',     1, 3, NOW(), NOW()),
  (5,  'Hackathon Sign-ups Open',          'hackathon-sign-ups-open',          'Registration for the annual internal hackathon is now open. Form your teams!',          2, 2, NOW(), NOW()),
  (6,  'Friday Game Night',                'friday-game-night',                'Join us this Friday evening for board games, snacks, and good company in the lounge.',   2, 4, NOW(), NOW()),
  (7,  'Best Tools for Remote Work',       'best-tools-for-remote-work',       'Share your favorite apps and setups for staying productive while working remotely.',     3, 5, NOW(), NOW()),
  (8,  'Coffee vs Tea: The Office Debate', 'coffee-vs-tea-the-office-debate',  'Settle it once and for all: which fuels your workday better, coffee or tea?',            3, 2, NOW(), NOW()),
  (9,  'Book Club: June Pick',             'book-club-june-pick',              'We are choosing the book for June. Drop your suggestions in the comments.',              3, 3, NOW(), NOW()),
  (10, 'Scheduled Maintenance This Weekend','scheduled-maintenance-this-weekend','Internal systems will be down for maintenance on Saturday from 10pm to 2am.',          4, 1, NOW(), NOW()),
  (11, 'Parking Lot Closure Notice',       'parking-lot-closure-notice',       'The north parking lot will be closed next Monday for resurfacing. Plan accordingly.',    4, 4, NOW(), NOW()),
  (12, 'Welcome Our New Teammates',        'welcome-our-new-teammates',        'Please join us in welcoming the newest members who joined this month. Say hello!',       1, 5, NOW(), NOW());

-- Comments across the new posts, authored by a mix of users
INSERT INTO comments (id, content, post_id, author_id, created_at) VALUES
  (1,  'Thanks for the recap, very helpful!',                         3,  2, NOW()),
  (2,  'Could we get the slides shared as well?',                     3,  4, NOW()),
  (3,  'Great quarter, congrats everyone.',                           3,  5, NOW()),
  (4,  'The new wing looks fantastic.',                               4,  1, NOW()),
  (5,  'Finally more meeting rooms!',                                 4,  2, NOW()),
  (6,  'Is the break area open already?',                             4,  5, NOW()),
  (7,  'Count me in for the hackathon!',                              5,  3, NOW()),
  (8,  'Looking for a teammate with backend experience.',            5,  4, NOW()),
  (9,  'What is the project theme this year?',                        5,  1, NOW()),
  (10, 'Game night sounds great, I will bring snacks.',              6,  2, NOW()),
  (11, 'Can we add a chess table?',                                   6,  5, NOW()),
  (12, 'See you all there!',                                          6,  3, NOW()),
  (13, 'I swear by my standing desk and noise-cancelling headphones.',7,  2, NOW()),
  (14, 'A good webcam makes a huge difference on calls.',             7,  1, NOW()),
  (15, 'Pomodoro timers keep me focused.',                            7,  4, NOW()),
  (16, 'Coffee, no contest.',                                         8,  3, NOW()),
  (17, 'Tea all the way, especially green tea.',                      8,  5, NOW()),
  (18, 'Why not both?',                                               8,  1, NOW()),
  (19, 'I vote for a sci-fi novel this month.',                       9,  4, NOW()),
  (20, 'Happy to host the discussion.',                               9,  2, NOW()),
  (21, 'Thanks for the heads-up on maintenance.',                     10, 3, NOW()),
  (22, 'Will the VPN be affected too?',                               10, 5, NOW()),
  (23, 'Noted, I will park on the south side.',                       11, 1, NOW()),
  (24, 'How long will the closure last?',                             11, 2, NOW()),
  (25, 'Welcome aboard, everyone!',                                   12, 4, NOW());

-- Advance identity sequences past the explicitly-inserted ids so future inserts don't collide
SELECT setval('users_id_seq',    (SELECT MAX(id) FROM users));
SELECT setval('posts_id_seq',    (SELECT MAX(id) FROM posts));
SELECT setval('comments_id_seq', (SELECT MAX(id) FROM comments));
