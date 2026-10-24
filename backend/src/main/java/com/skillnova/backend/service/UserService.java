package com.skillnova.backend.service;

import com.skillnova.backend.dto.RegisterRequest;
import com.skillnova.backend.entity.User;
import com.skillnova.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    /**
     * Placeholder registration path kept compiling against the Phase 3 User
     * entity. It does not yet validate input, hash the password, or reject
     * duplicate emails - that full implementation lands in Phase 4
     * (Registration API). The password is stored as-is only temporarily.
     */
    public String register(RegisterRequest request) {

        User user = User.builder()
                .fullName(request.fullName())
                .email(request.email())
                .passwordHash(request.password())
                .build();

        userRepository.save(user);

        return "User Registered Successfully";
    }
}
