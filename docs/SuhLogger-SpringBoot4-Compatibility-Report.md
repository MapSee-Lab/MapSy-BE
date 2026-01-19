# SuhLogger 라이브러리 Spring Boot 3.x/4.x 호환성 문제 보고서

## 1. 문제 요약

### 발생 오류
```
org.springframework.beans.factory.BeanDefinitionStoreException:
Failed to process import candidates for configuration class
[me.suhsaechan.suhlogger.config.SuhLoggerAutoConfiguration]

Caused by: java.lang.ClassNotFoundException:
org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
```

### 영향 범위
- **라이브러리**: `me.suhsaechan:suh-logger:1.3.11`
- **영향받는 환경**: Spring Boot 4.0.1 + Spring Framework 7.0.2
- **정상 동작 환경**: Spring Boot 3.x

---

## 2. 근본 원인 분석

### Spring Boot 4.x의 모듈화 (Modularization)

Spring Boot 4.0은 `spring-boot-autoconfigure` JAR을 작은 모듈로 분할하는 **획기적인 구조 변경**을 도입했습니다.

#### 변경 전 (Spring Boot 3.x)
```
spring-boot-autoconfigure (단일 JAR, ~2MB)
└── org.springframework.boot.autoconfigure.security.servlet
    └── SecurityAutoConfiguration.class  ✅ 존재
```

#### 변경 후 (Spring Boot 4.x)
```
spring-boot-autoconfigure (기본 JAR)
├── org.springframework.boot.autoconfigure (코어만 포함)

spring-boot-security (별도 모듈)
└── org.springframework.boot.security
    └── autoconfigure
        └── SecurityAutoConfiguration.class  ✅ 여기로 이동
```

### 패키지 경로 변경

| 버전 | SecurityAutoConfiguration 패키지 경로 |
|------|----------------------------------------|
| Spring Boot 3.x | `org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration` |
| Spring Boot 4.x | `org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration` |

---

## 3. SuhLogger 라이브러리 수정 필요 사항

### 3.1 현재 문제가 되는 코드 (추정)

SuhLogger의 `SuhLoggerAutoConfiguration` 클래스에서 다음과 같은 코드가 있을 것으로 추정:

```java
// ❌ Spring Boot 3.x 전용 - 4.x에서 ClassNotFoundException 발생
@AutoConfiguration(before = SecurityAutoConfiguration.class)
// 또는
@ConditionalOnClass(name = "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration")
```

### 3.2 수정 방안

#### 방안 A: 조건부 클래스 로딩 (권장)

```java
@AutoConfiguration
@ConditionalOnClass(name = {
    "org.springframework.security.core.Authentication"  // 공통 클래스 사용
})
public class SuhLoggerAutoConfiguration {
    // ...
}
```

#### 방안 B: 버전별 분기 처리

```java
@AutoConfiguration
public class SuhLoggerAutoConfiguration {

    // Spring Boot 버전 감지
    private static final boolean IS_SPRING_BOOT_4 = isSpringBoot4OrLater();

    private static boolean isSpringBoot4OrLater() {
        try {
            // Spring Boot 4.x 모듈화된 클래스 확인
            Class.forName("org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
```

#### 방안 C: 멀티 모듈 빌드 (가장 깔끔)

```
suh-logger/
├── suh-logger-core/           # 핵심 로직 (Spring 버전 무관)
├── suh-logger-spring-boot-3/  # Spring Boot 3.x 자동 설정
└── suh-logger-spring-boot-4/  # Spring Boot 4.x 자동 설정
```

### 3.3 META-INF 설정 파일 수정

#### Spring Boot 3.x (spring.factories)
```properties
# META-INF/spring.factories
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
me.suhsaechan.suhlogger.config.SuhLoggerAutoConfiguration
```

#### Spring Boot 4.x (AutoConfiguration.imports)
```
# META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
me.suhsaechan.suhlogger.config.SuhLoggerAutoConfiguration
```

**호환성 유지**: 두 파일 모두 유지하면 Spring Boot 3.x와 4.x 모두 지원 가능

---

## 4. 상세 수정 가이드

### 4.1 build.gradle 수정

```gradle
plugins {
    id 'java-library'
    id 'maven-publish'
}

// Spring Boot 3.x와 4.x 모두 지원하기 위한 의존성 설정
dependencies {
    // 공통 Spring 의존성 (컴파일 전용)
    compileOnly 'org.springframework.boot:spring-boot-autoconfigure'
    compileOnly 'org.springframework.security:spring-security-core'

    // Spring Boot 버전에 따라 선택적으로 로드됨
    compileOnly 'org.springframework.boot:spring-boot-security'  // 4.x용

    // 테스트
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}

// 버전 범위 지정
ext {
    springBootVersionRange = '[3.0.0, 5.0.0)'  // 3.x ~ 4.x 지원
}
```

### 4.2 AutoConfiguration 클래스 수정

```java
package me.suhsaechan.suhlogger.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * SuhLogger 자동 설정
 * Spring Boot 3.x 및 4.x 모두 지원
 */
@AutoConfiguration
@ConditionalOnClass(name = "org.springframework.security.core.Authentication")
public class SuhLoggerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SuhLoggerConfigurer suhLoggerConfigurer() {
        return new SuhLoggerConfigurer();
    }

    // SecurityAutoConfiguration 직접 참조 제거
    // 대신 Spring Security 공통 클래스 사용
}
```

### 4.3 조건부 Security 설정 (필요시)

```java
package me.suhsaechan.suhlogger.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;

/**
 * Security 관련 로깅 설정
 * Spring Security가 있을 때만 활성화
 */
@Configuration
@ConditionalOnClass(name = "org.springframework.security.core.context.SecurityContextHolder")
public class SuhLoggerSecurityConfig {

    // Spring Boot 버전과 무관하게 동작하는 Security 로깅 설정
    // SecurityAutoConfiguration을 직접 참조하지 않음
}
```

---

## 5. Spring Boot 3.x vs 4.x 주요 API 변경 요약

### 5.1 Security 관련 변경

| 항목 | Spring Boot 3.x | Spring Boot 4.x |
|------|-----------------|-----------------|
| SecurityAutoConfiguration 패키지 | `o.s.b.autoconfigure.security.servlet` | `o.s.b.security.autoconfigure` |
| WebSecurityConfigurerAdapter | Deprecated | **제거됨** |
| authorizeRequests() | Deprecated | **제거됨** |
| authorizeHttpRequests() | 권장 | 필수 |
| and() 메서드 | 사용 가능 | **제거됨** (Lambda DSL 필수) |

### 5.2 모듈 의존성 변경

```gradle
// Spring Boot 3.x
implementation 'org.springframework.boot:spring-boot-starter-security'

// Spring Boot 4.x (추가 필요시)
implementation 'org.springframework.boot:spring-boot-starter-security'
implementation 'org.springframework.boot:spring-boot-security'  // 자동 설정 모듈
```

---

## 6. 테스트 전략

### 6.1 멀티 버전 테스트 설정

```gradle
// build.gradle
test {
    useJUnitPlatform()
}

// Spring Boot 3.x 테스트
task testSpringBoot3(type: Test) {
    systemProperty 'spring.boot.version', '3.4.0'
}

// Spring Boot 4.x 테스트
task testSpringBoot4(type: Test) {
    systemProperty 'spring.boot.version', '4.0.1'
}
```

### 6.2 호환성 테스트 코드

```java
@SpringBootTest
class SuhLoggerCompatibilityTest {

    @Test
    void contextLoads() {
        // Spring Boot 버전에 관계없이 컨텍스트 로드 성공 확인
    }

    @Test
    void suhLoggerAutoConfigurationLoaded() {
        // SuhLoggerAutoConfiguration 빈 존재 확인
    }
}
```

---

## 7. 마이그레이션 체크리스트

### SuhLogger 라이브러리 개발자용

- [ ] `SecurityAutoConfiguration` 직접 import 제거
- [ ] 공통 Spring Security 클래스 사용으로 변경 (`Authentication`, `SecurityContextHolder` 등)
- [ ] `META-INF/spring.factories` 유지 (Spring Boot 3.x 호환)
- [ ] `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 추가 (Spring Boot 4.x)
- [ ] 멀티 버전 테스트 추가
- [ ] 버전 번호 업데이트 (예: 1.3.11 → 1.4.0 또는 2.0.0)
- [ ] CHANGELOG 업데이트
- [ ] Nexus 저장소에 새 버전 배포

### SuhLogger 사용자용 (MapSy-BE)

- [ ] SuhLogger 버전 업데이트 (호환 버전 배포 후)
- [ ] 또는 임시로 SuhLogger 의존성 제거
- [ ] Spring Boot 4.x 마이그레이션 가이드 확인
- [ ] `spring-boot-starter-security-test` 의존성 추가 (테스트용)

---

## 8. 임시 해결 방안 (MapSy-BE)

SuhLogger 라이브러리 업데이트 전까지 사용할 수 있는 임시 방안:

### 방안 1: SuhLogger 제외

```gradle
// MS-Common/build.gradle
// 임시로 주석 처리
// api "me.suhsaechan:suh-logger:1.3.11"
// api "me.suhsaechan:suh-api-log:1.1.5"
```

### 방안 2: Spring Boot 다운그레이드

```gradle
// 루트 build.gradle
plugins {
    id 'org.springframework.boot' version '3.4.0'  // 4.0.1 → 3.4.0
}
```

### 방안 3: classic autoconfigure 모듈 추가 (실험적)

```gradle
// Spring Boot 4.x에서 레거시 호환성 모듈 추가
implementation 'org.springframework.boot:spring-boot-autoconfigure-classic'
```

---

## 9. 참고 자료

- [Spring Boot 4.0 Migration Guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide)
- [Modularizing Spring Boot (공식 블로그)](https://spring.io/blog/2025/10/28/modularizing-spring-boot/)
- [Spring Boot 4 Modularization Fix](https://www.danvega.dev/blog/2025/12/12/spring-boot-4-modularization)
- [Spring Security Migration to 7.0](https://docs.spring.io/spring-security/reference/migration/index.html)
- [Spring Boot Security Auto-Configuration | Baeldung](https://www.baeldung.com/spring-boot-security-autoconfiguration)

---

## 10. 결론

**핵심 문제**: SuhLogger 1.3.11이 Spring Boot 3.x 전용 패키지 경로(`org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration`)를 하드코딩하고 있음

**해결책**: SuhLogger 라이브러리에서 `SecurityAutoConfiguration` 직접 참조를 제거하고, 버전 무관한 공통 Spring Security 클래스 사용으로 변경

**권장 버전 정책**:
- SuhLogger 1.x: Spring Boot 3.x 전용
- SuhLogger 2.x: Spring Boot 3.x + 4.x 모두 지원

---

*작성일: 2026-01-18*
*대상 프로젝트: MapSy-BE*
*관련 라이브러리: me.suhsaechan:suh-logger:1.3.11*
