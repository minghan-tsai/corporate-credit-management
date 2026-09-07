-- Execute this statement before the Maker-Checker HTTP requests in api-test.http.
-- It simulates a maker whose role was later changed from RM to REVIEWER.
UPDATE app_user
SET role = 'REVIEWER'
WHERE username = 'stage5_rm';

-- Execute this statement after the Maker-Checker HTTP request to restore the test account.
UPDATE app_user
SET role = 'RM'
WHERE username = 'stage5_rm';
