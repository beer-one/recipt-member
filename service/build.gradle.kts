dependencies {
    // Spring Security
    implementation("org.springframework.security:spring-security-crypto")

    // jwt
    implementation("io.jsonwebtoken:jjwt:0.9.1")

    // redis
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")
}