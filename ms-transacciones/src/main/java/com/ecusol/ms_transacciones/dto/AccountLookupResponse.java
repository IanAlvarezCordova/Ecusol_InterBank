package com.ecusol.ms_transacciones.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountLookupResponse implements Serializable {

    @JsonProperty("status")
    private String status;

    @JsonProperty("data")
    private DataInfo data;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public static class DataInfo implements Serializable {
        @JsonProperty("exists")
        private boolean exists;

        @JsonProperty("ownerName")
        private String ownerName;

        @JsonProperty("accountName")
        private String accountName;

        @JsonProperty("currency")
        private String currency;

        @JsonProperty("status")
        private String status;

        @JsonProperty("mensaje")
        private String mensaje;
    }
}
