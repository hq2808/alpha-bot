package com.alphabot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import java.util.Collection;

@Data
@Builder
@Schema(description = "Batch real-time stock quotes response")
public class RealTimeQuotesResponse {
    @Schema(description = "List of stock quotes")
    private Collection<Object> data;
}
