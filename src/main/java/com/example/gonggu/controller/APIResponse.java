package com.example.gonggu.controller;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Getter
@Setter
@ToString
@Component
public class APIResponse {
    public HttpStatus status;
    public String message;
    public Object acceptJson;
    public Object returnJson;
}