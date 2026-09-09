package com.minghan.credit.dto;

import java.util.List;

public record CreditApplicationPageResponse(
        List<CreditApplicationResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last) {

    public CreditApplicationPageResponse {
        content = List.copyOf(content);
    }
}
