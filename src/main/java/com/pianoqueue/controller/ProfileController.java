package com.pianoqueue.controller;

import com.pianoqueue.dto.ProfileDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    @Value("${tiktok.handle}")
    private String handle;

    @Value("${tiktok.bio}")
    private String bio;

    @GetMapping
    public ProfileDto getProfile() {
        return new ProfileDto(handle, bio, "https://tiktok.com/@" + handle);
    }
}
