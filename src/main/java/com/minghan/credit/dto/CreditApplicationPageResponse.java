package com.minghan.credit.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

public record CreditApplicationPageResponse(

        @Schema(description = "Credit applications in the current page") List<CreditApplicationResponse> content,

        @Schema(description = "Current page number, starting from 0", example = "0") int page,

        @Schema(description = "Number of records per page", example = "20") int size,

        @Schema(description = "Total number of credit applications", example = "45") long totalElements,

        @Schema(description = "Total number of pages", example = "3") int totalPages,

        @Schema(description = "Whether this is the first page", example = "true") boolean first,

        @Schema(description = "Whether this is the last page", example = "false") boolean last

) {

    public CreditApplicationPageResponse {
        content = List.copyOf(content);
    }
}