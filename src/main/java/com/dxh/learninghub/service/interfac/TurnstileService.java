package com.dxh.learninghub.service.interfac;

public interface TurnstileService {
    void verify(String token, String expectedAction);
}
