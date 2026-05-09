package com.koinonia.backend.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UnreadCountResponse {
    private long count;
}
