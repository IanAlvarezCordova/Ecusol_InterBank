package com.ecusol.ms_transacciones.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnRequestDTO {

    @JsonProperty("header")
    private Header header;

    @JsonProperty("body")
    private Body body;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Header {
        @JsonProperty("messageId")
        private String messageId;

        @JsonProperty("creationDateTime")
        private String creationDateTime;

        @JsonProperty("originatingBankId")
        private String originatingBankId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Body {
        @JsonProperty("returnInstructionId")
        private String returnInstructionId;

        @JsonProperty("originalInstructionId")
        private String originalInstructionId;

        @JsonProperty("returnReason")
        private String returnReason;

        @JsonProperty("returnAmount")
        private Amount returnAmount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Amount {
        @JsonProperty("currency")
        private String currency;

        @JsonProperty("value")
        private BigDecimal value;
    }
}