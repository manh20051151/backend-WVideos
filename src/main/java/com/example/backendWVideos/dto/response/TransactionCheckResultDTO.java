package com.example.backendWVideos.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TransactionCheckResultDTO {
    boolean found;
    String status;
    String message;
    SepayTransactionDTO matchedTransaction;
    String searchDescription;
    int totalTransactionsChecked;
}
