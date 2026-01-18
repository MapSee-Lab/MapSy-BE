package kr.suhsaechan.mapsy.web.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Jackson 설정
 * Spring Boot 4.0.1의 JsonMapper를 ObjectMapper로 제공하여 기존 코드 호환성 유지
 */
@Configuration
public class JacksonConfig {

  /**
   * ObjectMapper 빈 정의
   * Spring Boot 4.0.1에서 JsonMapper가 자동 구성되면 이를 활용하고,
   * 없으면 JsonMapper.builder()로 직접 생성합니다.
   *
   * @return ObjectMapper 빈
   */
  @Bean
  @Primary
  @ConditionalOnMissingBean(ObjectMapper.class)
  public ObjectMapper objectMapper() {
    // JsonMapper.builder()로 ObjectMapper 생성
    // JsonMapper는 ObjectMapper의 서브클래스이므로 호환성 보장
    return JsonMapper.builder()
        .findAndAddModules()
        .build();
  }
}
