package com.dxh.learninghub.configuration;

import com.dxh.learninghub.entity.Role;
import com.dxh.learninghub.entity.User;
import com.dxh.learninghub.enums.RoleEnum;
import com.dxh.learninghub.repo.RoleRepository;
import com.dxh.learninghub.repo.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Set;

@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@Slf4j
public class ApplicationInitConfig {


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    //lấy tt ng tạo
    @Bean
    public AuditorAware<String> auditorProvider() {
        return new AuditorAwareImpl();
    }

    @Bean
    ApplicationRunner applicationRunner(UserRepository userRepository, RoleRepository roleRepository,PasswordEncoder passwordEncoder){
        return args -> {
            if (userRepository.findByUsername("admin").isEmpty()){
                Role adminRole = roleRepository.findByName(RoleEnum.ADMIN.name())
                        .orElseGet(() -> roleRepository.save(
                                Role.builder()
                                        .name(RoleEnum.ADMIN.name())
                                        .description("Admin")
                                        .build()
                        ));

                Role userRole = roleRepository.findByName(RoleEnum.USER.name())
                        .orElseGet(() -> roleRepository.save(
                                Role.builder()
                                        .name(RoleEnum.USER.name())
                                        .description("User")
                                        .build()
                        ));

                Role teacherRole = roleRepository.findByName(RoleEnum.TEACHER.name())
                        .orElseGet(() -> roleRepository.save(
                                Role.builder()
                                        .name(RoleEnum.TEACHER.name())
                                        .description("Teacher")
                                        .build()
                        ));

                Set<Role> roles = new HashSet<>();
                roles.add(adminRole);
                roles.add(userRole);
                roles.add(teacherRole);

                User user = User.builder()
                        .username("admin")
                        .fullName("Đặng Xuân Hùng")
                        .phoneNumber("0911581476")
                        .email("hunglockedk4@gmail.com")
                        .enabled(true)
                        .password(passwordEncoder.encode("admin"))
                        .roles(roles)
                        .build();

                userRepository.save(user);
                log.warn("admin user has been created with default password: admin, please change it");
            }
        };
    }
}
