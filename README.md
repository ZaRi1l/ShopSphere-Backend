## ShopSphere Project
<div style="text-align: center;">
    <img alt="shopshere_main" src="https://github.com/user-attachments/assets/bf3efd80-989e-4ac8-837d-9eff685e74c5" />
</div>

## 프로젝트 소개
### '왜?'라는 질문에서 시작된 풀스택 커머스 프로젝트프로젝트, ShopSphere입니다.

단순히 기능을 나열하는 것을 넘어, **'어떻게 하면 사용자가 더 편하게 소통하고 구매할 수 있을까?'** 라는 고민에서 출발했습니다. 
이 고민의 해답을 찾기 위해, 저희는 Spring Boot의 강력한 백엔드와 React의 동적인 프론트엔드를 결합하여 사용자 중심의 플랫폼을 구축했습니다.

백엔드에서는 **Spring Security**를 이용한 인증/인가 처리와 **JPA**를 통한 효율적인 데이터 관리를, 
프론트엔드에서는 **WebSocket**을 이용한 실시간 채팅과 **Toss Payments 연동**을 통해 끊김 없는(Seamless) 사용자 경험을 구현하며 풀스택 개발 역량을 종합적으로 녹여냈습니다.
##### 사이트 주소: https://shopsphere123.duckdns.org
##### 설치 방법: <a href="/dosc/설치방법/README.md" target="_blank">설치방법</a>
<br>

### 주요 기능 (Key Features)
| 기능 | 설명 | 관련 기술 |
| :---: | --- | :---: |
| **상세 검색 기능** | 단순 키워드 검색을 넘어 **카테고리별 필터링** 기능을 제공하여, 사용자가 원하는 상품을 더욱 빠르고 정확하게 찾을 수 있도록 구현했습니다. | `Spring Boot`, `JPA` |
| **실시간 1:1 문의** | WebSocket 기술을 기반으로 구매자와 판매자 간의 **실시간 채팅 기능**을 구현했습니다. 사용자는 상품에 대해 즉각적으로 문의하고 답변을 받으며 소통 경험을 극대화할 수 있습니다. | `WebSocket`, `Stomp.js` |
| **소셜 로그인 & 간편 결제** | 사용자의 편의성을 높이기 위해 카카오 소셜 로그인을 도입하고, 토스페이먼츠 API를 연동하여 **간편하고 안전한 결제 시스템**을 구축했습니다. | `OAuth 2.0`, `Toss Payments` |
| **판매자 대시보드** | 판매자를 위한 별도의 관리 페이지에서 상품 관리(등록/수정/삭제)는 물론, **그래프를 통해 판매 통계를 시각화**하여 직관적인 데이터 확인 및 판매 전략 수립을 지원합니다. | `React`, `Chart.js 등` |
| **보안 인증/인가** | **Spring Security**를 사용하여 견고한 인증 및 인가 시스템을 구현했습니다. 사용자의 역할(구매자/판매자)에 따라 접근 권한을 제어하여 시스템과 데이터를 안전하게 보호합니다. | `Spring Security` |

<br>
<br>

## 팀원 
|최진환|김동현|김윤진|박규태|
|:---:|:---:|:---:|:---:|
|<a href="https://github.com/trumanjinhwan" target="_blank"><img src="https://avatars.githubusercontent.com/u/190100768?v=4" height="150px"/><br>trumanjinhwan</a>|<a href="https://github.com/kimdonghyun296" target="_blank"><img src="https://avatars.githubusercontent.com/u/193192616?v=4" height="150px"/><br>kimdonghyun296</a>|<a href="https://github.com/yunndaeng" target="_blank"><img src="https://avatars.githubusercontent.com/u/193191038?v=4" height="150px"/><br>yunndaeng</a>|<a href="https://github.com/ZaRi1l" target="_blank"><img src="https://avatars.githubusercontent.com/u/133009070?v=4" height="150px"/><br>ZaRi1l</a>|

#### 맡은 역할
| 이름 |업무|
|:---:|---|
|최진환| 로그인/회원가입 프론트엔드, 로그인/회원가입 백엔드, 상품검색 프론트엔드, 상품검색 백엔드, 상품등록 프론트엔드, 상품정보 프론트엔드, 상품정보 백엔드, 상품구매 백엔드, 상품구매 프론트엔드, 리뷰&별점 프론트엔드, 배포/운영, CI/CD, ppt만들기, 유스케이스 다이어그램
|김동현|DB구축, 카카오 로그인 구현, 토스 페이먼츠 구현, 장바구니 프론트엔드, 구매내역 ID 띄우기, 시퀀스 다이어그램, 클래스 다이어그램, E-R 다이어그램, 해상도를 고려한 CSS 수정
|김윤진|상품정보 페이지, 장바구니 프론트엔드, 장바구니 벡엔드, 1:1 문의 구매자Ver, 요구사항, 깃 레파지토리 README.md
|박규태|로그인/회원가입 백엔드, 상품검색 백엔드, 상품등록 백엔드, 상품정보 백엔드, 상품구매 백엔드, 장바구니 백엔드, 리뷰&별점 백엔드, 판매 통계 백엔드, 판매 통계 프론트엔드, 상품등록 옵션, 1:1 문의 판매자Ver, 챗봇, 실시간 1:1 문의 웹소켓(프론트,백엔드)

## 개발환경
| Backend | Frontend | DB | VCS | CSP |
|:---:|:---:|:---:|:---:|:---:|
|<img src="https://img.icons8.com/color/96/spring-logo.png" width="100" alt="Spring Boot"/>|<img src="https://img.icons8.com/color/96/react-native.png" width="100" alt="React"/>|<img src="https://img.icons8.com/color/96/maria-db.png" width="100" alt="MariaDB"/>|<img src="https://img.shields.io/badge/GitHub-181717?style=for-the-badge&logo=github&logoColor=white"/>|<img src="https://img.shields.io/badge/AWS-232F3E?style=for-the-badge&logo=amazon-aws&logoColor=white"/>|
|Spring Boot|React|MariaDB|GitHub|AWS|

| Language | IDE | Build Tool |
|:---:|:---:|:---:|
|<img src="https://img.icons8.com/color/70/java-coffee-cup-logo.png" width="70" alt="Java"/> <img src="https://img.icons8.com/color/70/javascript.png" width="70" alt="JavaScript"/> <img src="https://img.icons8.com/color/70/html-5.png" width="70" alt="HTML"/> <img src="https://img.icons8.com/color/70/css3.png" width="70" alt="CSS"/>|<img src="https://img.icons8.com/color/96/visual-studio-code-2019.png" width="100" alt="VSCode"/>|<img src="https://simpleicons.org/icons/gradle.svg" width="70" alt="Gradle"/> <img src="https://img.shields.io/badge/npm-CB3837?style=for-the-badge&logo=npm&logoColor=white"/>|
|Java, JavaScript, HTML, CSS|VSCode|Gradle, NPM|

#### 개발환경 상세     
|환경|사용|버전|
|:---:|:---:|:---:|
| **OS** |<img src="https://img.shields.io/badge/OS%20Independent-gray?style=for-the-badge"/>|Windows, macOS, Linux 무관|
| **프론트엔드** | <img src="https://img.shields.io/badge/React-61DAFB?style=for-the-badge&logo=react&logoColor=white"/>|19.1.0|
| **라우팅** | <img src="https://img.shields.io/badge/React%20Router%20DOM-CA4245?style=for-the-badge&logo=react-router&logoColor=white"/>|7.6.0|
| **CSS** | <img src="https://img.shields.io/badge/Tailwind%20CSS-38B2AC?style=for-the-badge&logo=tailwind-css&logoColor=white"/>| - |
| **상태 관리** | <img src="https://img.shields.io/badge/zustand-000000?style=for-the-badge"/>| - |
| **결제** | <img src="https://img.shields.io/badge/Toss%20Payments-000000?style=for-the-badge"/>|1.9.1|
| **테스트** | <img src="https://img.shields.io/badge/Jest-C21325?style=for-the-badge&logo=jest&logoColor=white"/> <img src="https://img.shields.io/badge/Testing%20Library-E33332?style=for-the-badge"/>| - |
| **빌드** | <img src="https://img.shields.io/badge/CRACO-000000?style=for-the-badge"/> <img src="https://img.shields.io/badge/npm-CB3837?style=for-the-badge&logo=npm&logoColor=white"/>| - |
| **IDE** | <img src="https://img.shields.io/badge/Visual%20Studio%20Code-007ACC?style=for-the-badge&logo=visual-studio-code&logoColor=white"/>| - |
| **Server** | <img src="https://img.shields.io/badge/apache%20tomcat-%23F8DC75.svg?style=for-the-badge&logo=apache-tomcat&logoColor=black"/>|Spring Boot 내장 Tomcat|
| **CSP** | <img src="https://img.shields.io/badge/AWS-232F3E?style=for-the-badge&logo=amazon-aws&logoColor=white"/> | - |
| **VCS** | <img src="https://img.shields.io/badge/GitHub-181717?style=for-the-badge&logo=github&logoColor=white"/> | - |
| <hr> | <hr> | <hr> |
| **Backend** | <img src="https://img.shields.io/badge/Spring%20Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white"/> | `3.4.5` |
| **Language (BE)** | <img src="https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white"/>| JDK: `17`|
| **Build Tool (BE)** | <img src="https://img.shields.io/badge/Gradle-02303A.svg?style=for-the-badge&logo=gradle&logoColor=white"/> | - |
| <hr> | <hr> | <hr> |
| **Frontend** | <img src="https://img.shields.io/badge/React-20232A?style=for-the-badge&logo=react&logoColor=61DAFB"/>| React: `18.2.0` |
| **Language (FE)** | <img src="https://img.shields.io/badge/JavaScript-F7DF1E?style=for-the-badge&logo=javascript&logoColor=black"/> <img src="https://img.shields.io/badge/CSS3-1572B6?style=for-the-badge&logo=css3&logoColor=white"/> | ES6+, JSX |
| **Package Manager**| <img src="https://img.shields.io/badge/npm-CB3837?style=for-the-badge&logo=npm&logoColor=white"/> | - |
| **Library (FE)** | <img src="https://img.shields.io/badge/axios-5A29E4?style=for-the-badge&logo=axios&logoColor=white"/> <img src="https://img.shields.io/badge/stompjs-E33A2B?style=for-the-badge"/> | axios, @stomp/stompjs, sockjs-client |
| <hr> | <hr> | <hr> |
| **DB** | <img src="https://img.shields.io/badge/MariaDB-003545?style=for-the-badge&logo=mariadb&logoColor=white"/>| - |


<br><br>
## 요구사항

<div style="text-align: center;">
    <img height="500px" src="dosc/소프트웨어 설계/요구사항.png" alt="alt text" />
</div>

#### 요구사항 분석(기능 정리)
|요구사항|상세내용|
|:---:|---|
| 회원 관리 | 회원가입 & 로그인, 프로필 관리, 주문 내역 관리, 회원 탈퇴 |
| 상품 기능 | 상품 목록 조회, 상품 상세 페이지, 상품 검색 & 필터링, 상품 추천 |
| 장바구니 | 상품 담기 / 삭제, 수량 변경, 장바구니 관리, 총 금액 확인 |
| 주문 & 결제 | 주문서 작성, 결제 처리, 주문 내역 조회, 배송 관리 |
| 커뮤니티 & 지원 | 리뷰 작성/조회, 별점 평가, AI 챗봇 문의, 1:1 문의 |
| 판매자 기능 | 상품 관리, 판매 통계, 주문 관리, 고객 관리 |

<br><br>
## 구현 영상
<a href="/dosc/구현 영상/">구현 영상</a>

<br>

## 구현 사진

#### 메인화면
<div style="text-align: center;">
    <img alt="shopshere_main" src="/dosc/구현 사진/메인화면.png" />
</div>


<br>

#### 상품검색
<div style="text-align: center;">
    <img alt="shopshere_main" src="/dosc/구현 사진/상품검색1.png" />
</div>

<div style="text-align: center;">
    <img alt="shopshere_main" src="/dosc/구현 사진/상품검색2.png" />
</div>
<br>

#### 상품등록
 <div style="text-align: center;">
    <img alt="shopshere_main" src="/dosc/구현 사진/상품등록.png" />
</div>

<br>

#### 상품상세 페이지
 <div style="text-align: center;">
    <img alt="shopshere_main" src="/dosc/구현 사진/상품상세페이지.png" />
</div>

<br>


<br><br>

## 소프트웨어 설계

### UseCase Diagram
<div style="text-align: center;">
    <img alt="shopshere_main" src="/dosc/소프트웨어 설계/Usecase_구매자.png" />
</div>

<div style="text-align: center;">
    <img alt="shopshere_main" src="/dosc/소프트웨어 설계/Usecase_판매자.png" />
</div>



<br>

### Sequence Diagram
<div style="text-align: center;">
    <img alt="shopshere_main" src="/dosc/소프트웨어 설계/SequenceDiagram_상품구매.png" />
</div>

<div style="text-align: center;">
    <img alt="shopshere_main" src="/dosc/소프트웨어 설계/SequenceDiagram_상품검색.png" />
</div>

<div style="text-align: center;">
    <img alt="shopshere_main" src="/dosc/소프트웨어 설계/SequenceDiagram_상품등록.png" />
</div>

<br>

### Class Diagram
<div style="text-align: center;">
    <img alt="shopshere_main" src="/dosc/소프트웨어 설계/Class_User.png" />
</div>

<div style="text-align: center;">
    <img alt="shopshere_main" src="/dosc/소프트웨어 설계/Class_Product.png" />
</div>

<div style="text-align: center;">
    <img alt="shopshere_main" src="/dosc/소프트웨어 설계/Class_ProductCategory.png" />
</div>

<div style="text-align: center;">
    <img alt="shopshere_main" src="/dosc/소프트웨어 설계/Class_Cart.png" />
</div>

<div style="text-align: center;">
    <img alt="shopshere_main" src="/dosc/소프트웨어 설계/Class_Order.png" />
</div>

<div style="text-align: center;">
    <img alt="shopshere_main" src="/dosc/소프트웨어 설계/Class_Review.png" />
</div>

<div style="text-align: center;">
    <img alt="shopshere_main" src="/dosc/소프트웨어 설계/Class_상품등록.png" />
</div>

<div style="text-align: center;">
    <img alt="shopshere_main" src="/dosc/소프트웨어 설계/Class_파일업로드.png" />
</div>
 
<br>

### E-R Diagrame
 <div style="text-align: center;">
    <img alt="shopshere_main" src="/dosc/소프트웨어 설계/ERD.png" />
</div>

<br>

<br><br>
