\set ON_ERROR_STOP on

SELECT 'CREATE DATABASE customer_db'
WHERE NOT EXISTS (
    SELECT
    FROM pg_database
    WHERE datname = 'customer_db'
)\gexec

SELECT 'CREATE DATABASE loan_application_db'
WHERE NOT EXISTS (
    SELECT
    FROM pg_database
    WHERE datname = 'loan_application_db'
)\gexec

SELECT 'CREATE DATABASE credit_assessment_db'
WHERE NOT EXISTS (
    SELECT
    FROM pg_database
    WHERE datname = 'credit_assessment_db'
)\gexec

SELECT 'CREATE DATABASE offer_db'
WHERE NOT EXISTS (
    SELECT
    FROM pg_database
    WHERE datname = 'offer_db'
)\gexec

SELECT 'CREATE DATABASE notification_db'
WHERE NOT EXISTS (
    SELECT
    FROM pg_database
    WHERE datname = 'notification_db'
)\gexec

SELECT 'CREATE DATABASE audit_db'
WHERE NOT EXISTS (
    SELECT
    FROM pg_database
    WHERE datname = 'audit_db'
)\gexec
