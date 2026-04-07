package com.example.backendWVideos.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SepayTransactionDTO {
    String id;
    
    @JsonProperty("transaction_date")
    String transactionDate;
    
    @JsonProperty("transactionDateParse")
    String transactionDateParse;
    
    @JsonProperty("referenceNumber")
    String referenceNumber;
    
    @JsonProperty("accountNumber")
    String accountNumber;
    
    @JsonProperty("subAccount")
    String subAccount;
    
    @JsonProperty("amount_in")
    String amountIn;
    
    @JsonProperty("amountOut")
    String amountOut;
    
    @JsonProperty("accumulated")
    String accumulated;
    
    @JsonProperty("transaction_content")
    String transactionContent;
    
    @JsonProperty("transactionType")
    String transactionType;
    
    String status;
}
