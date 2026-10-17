package com.skillnova.backend.service;

import com.skillnova.backend.dto.RegisterRequest;
import com.skillnova.backend.entity.User;
import com.skillnova.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public String register(RegisterRequest request) {

        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .password(request.password())
                .createdAt(LocalDateTime.now())
                .build();

        userRepository.save(user);

        return "User Registered Successfully";
    }
}
