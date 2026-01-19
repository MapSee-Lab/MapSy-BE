package kr.suhsaechan.mapsy.web.config;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = "kr.suhsaechan.mapsy")
@EntityScan(basePackages = "kr.suhsaechan.mapsy")
public class JpaConfig {

}
