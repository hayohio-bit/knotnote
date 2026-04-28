-- KnotNote MySQL 초기화 스크립트
-- 실행: mysql -u root -p < db/init.sql

CREATE DATABASE IF NOT EXISTS knotnote
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

-- 전용 유저 생성 (비밀번호는 setup.ps1 .env.local 기준)
CREATE USER IF NOT EXISTS 'knotnote'@'localhost' IDENTIFIED BY 'knotnote1234!';
GRANT ALL PRIVILEGES ON knotnote.* TO 'knotnote'@'localhost';
FLUSH PRIVILEGES;

USE knotnote;

-- JPA ddl-auto: update 이므로 테이블은 Spring Boot 기동 시 자동 생성됨
-- 아래는 수동 확인용 쿼리
SELECT SCHEMA_NAME, DEFAULT_CHARACTER_SET_NAME
FROM information_schema.SCHEMATA
WHERE SCHEMA_NAME = 'knotnote';
