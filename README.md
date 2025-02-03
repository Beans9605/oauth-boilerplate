# Getting Start
## Overview
이 프로젝트는 synchronization spring boot 3 에서 기초적으로 OAuth2 Client 의 사용법을 공유하고 있습니다.
심화된 내용은 다른 보일러 플레이트를 참고하시기 바랍니다.

## Used
- keycloak
- oauth client

심플하게 두 가지 테크닉만 사용하여 동작시키게 했으며, keycloak 에 대한 docker-compose 와 해당 keycloak 에 setting 할
realm-export 값도 포함하고 있습니다.

## How to build
1. 우선 docker-compose 를 up 하여 postgresql 과 keycloak 을 개발환경 상태로 띄워주시고, keycloak 에 admin 계정으로 로그인 (ID: admin, PW: password) 하여 realm create 시에 keycloak-settings/test-realm-export.json 파일을 import 해주세요.
2. 설정이 완료 됐다면 해당 spring boot 프로젝트를 실행하여 login 시도를 해보세요.
   - userId: niceman, pw: password
3. 정상적으로 로그인이 되는지, 로그아웃이 되는지, token 이 발급되는지 확인해주세요.