INSERT INTO "CLIENT"("ID", "SECRET_HASH", "SCOPE", "TOKEN_TTL", "LAST_MODIFIED", "VERSION")
VALUES (
  '1901498a-9ded-4eba-82e77e9946c0e231',
  '1000:8ba36faff318fbe72abae18838197d58bb507a8bb5c3e049:18df1ece6fc350f6b55e19c906acc7f9b2aef79bec27d50b',
  'jetonmatik:clients:modify foo:user:search foo:user:modify',
  10800,
  TIMESTAMP '2015-01-01 00:00:00',
  1);

INSERT INTO "CLIENT"("ID", "SECRET_HASH", "SCOPE", "TOKEN_TTL", "LAST_MODIFIED", "VERSION")
VALUES (
  '331b2eeb-d0c1-4d58-ae9cfe37f84a4214',
  '1000:109c4841a5241ce6c1a395d35228113de9861a24f71452a1:475c60fdcfa8aab16e15fdaeb6e4210220ae2aa309e078b6',
  'boo:user:search boo:user:modify',
  10800,
  TIMESTAMP '2015-01-01 00:00:00',
  1);