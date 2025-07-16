# ShopSphere 쇼핑몰 프로젝트 설치 방법

## 1. 프론트엔드 환경 설정
### 1.1 Node.js 설치
- Node.js LTS 버전을 설치합니다 (현재 프로젝트는 Node.js 18.x 이상을 권장)
- npm 버전 확인: `npm -v`

### 2.2 프로젝트 클론 및 설치
```bash
# 프로젝트 클론
$ git clone https://github.com/Sahmyook-4-team/ShopSphere-Frontend.git

# 프로젝트 디렉토리로 이동
$ cd ShopSphere-Frontend

# 의존성 설치
$ npm install
```

### 1.3 프로젝트 실행
```bash
# 개발 서버 실행
$ npm start

# 프로덕션 빌드
$ npm run build
```

## 3. 백엔드 설치 방법

### 3.1 환경 준비
1. Java JDK 17 이상 설치
2. JAVA_HOME 환경 변수 설정
3. MariaDB 10.6 이상 설치

### 3.2 프로젝트 클론
```bash
# 백엔드 프로젝트 클론
$ git clone https://github.com/Sahmyook-4-team/ShopSphere-Backend.git
```

### 3.3 데이터베이스 설정
#### 3.3.1 MariaDB 설치 (macOS)
```bash
# Homebrew를 통해 MariaDB 설치
$ brew install mariadb

# MariaDB 서비스 시작
$ brew services start mariadb

# MariaDB root 비밀번호 설정
$ mysql_secure_installation
```

#### 3.3.2 데이터베이스 생성 및 사용자 생성
```sql
-- MySQL/MariaDB 접속
mysql -u root -p

-- 데이터베이스 생성
CREATE DATABASE shopsphere CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 사용자 생성 및 권한 부여
CREATE USER 'shopsphere'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON shopsphere.* TO 'shopsphere'@'localhost';
```

### 2.4 Gradle 설정
- Gradle 8.0 이상을 설치합니다
- 프로젝트 빌드
```bash
# 프로젝트 디렉토리로 이동
$ cd ShopSphere-Backend

# 의존성 다운로드 및 빌드
$ ./gradlew build
```

## 4. 실행 방법

### 4.1 개발 환경
1. 프론트엔드 실행: `npm start`
2. 백엔드 실행: `./gradlew bootRun`

### 4.2 프로덕션 환경
1. 프론트엔드 빌드: `npm run build`
2. 백엔드 빌드: `./gradlew build`
3. Docker 사용 시:
```bash
# 프론트엔드 도커 이미지 빌드
$ docker build -t shopsphere-frontend .

# 백엔드 도커 이미지 빌드
$ docker build -t shopsphere-backend .

# 도커 컴포즈로 실행
$ docker-compose up
```

## 5. 주요 기술 스택

### 프론트엔드
- React 19.1.0
- React Router DOM 7.6.0
- Tailwind CSS
- Zustand (상태 관리)
- Jest & Testing Library
- CRACO (React Scripts 커스터마이징)

### 백엔드
- Spring Boot 3.4.5
- Spring Data JPA
- Spring Security
- WebSocket
- AWS SDK

### 데이터베이스
- MariaDB

### 클라우드 서비스
- AWS

### 버전 관리
- GitHub

### 빌드 도구
- Gradle (백엔드)
- npm (프론트엔드)

### API & 라이브러리
- Google GenAI 1.5.0 (챗봇)
- tosspayments-sdk 1.9.1 (결제)
- Kakao SDK 1.43.6 (소셜 로그인)
