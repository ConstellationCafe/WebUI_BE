//package com.help.backend.domain.repository.controller;
//
//import com.help.global.common.response.ApiResponse;
//import com.help.global.jwt.CustomUser;
//import com.help.backend.domain.repository.dto.request.repository.MembershipDto;
//import com.help.backend.domain.repository.service.MembershipService;
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import org.springframework.web.bind.annotation.*;
//
//@RestController
//@RequestMapping("/api/repository/membership")
//@Slf4j
//@RequiredArgsConstructor
//public class MembershipController {
//    private final MembershipService membershipService;
//
//    @GetMapping("/get")
//    public ApiResponse<?> getMembership(@AuthenticationPrincipal CustomUser user) {
//        return membershipService.getMembership(user);
//    }
//
//    @PostMapping("/save")
//    public ApiResponse<?> saveAll(
//            @Valid @RequestBody final MembershipDto membership
//    ) {
//        return membershipService.save(membership);
//    }
//}