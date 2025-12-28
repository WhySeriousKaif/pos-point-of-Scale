package com.molla.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import com.molla.exceptions.UserException;
import com.molla.payload.dto.UserDto;
import com.molla.payload.response.AuthResponse;
import com.molla.service.AuthService;

@RestController
@RequestMapping("/auth")
public class AuthController {
    @Autowired
    private AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signUpHandler(@RequestBody UserDto userDto) throws UserException{

        return new ResponseEntity<>(authService.signUp(userDto), HttpStatus.CREATED);
    }
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> loginHandler(@RequestBody UserDto userDto){

        return new ResponseEntity<>(authService.login(userDto), HttpStatus.OK);
    }
}
