package by.mashnyuk.paymentServiceDemo.mcp;

import by.mashnyuk.paymentServiceDemo.model.dto.response.ExceededTransactionResponseDto;
import by.mashnyuk.paymentServiceDemo.model.dto.response.LimitResponseDto;
import by.mashnyuk.paymentServiceDemo.service.LimitService;
import by.mashnyuk.paymentServiceDemo.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClientToolsMcp {

    private final LimitService limitService;
    private final TransactionService transactionService;

    @Description("Get all established expense limits for a specific 10-digit bank account")
    public List<LimitResponseDto> getClientLimits(String account) {
        log.info("MCP Tool: getClientLimits called for account {}", account);
        return limitService.getAllLimits(account).stream()
                .map(limit -> LimitResponseDto.builder()
                        .accountFrom(limit.getAccountFrom())
                        .limitSum(limit.getLimitSum())
                        .limitDatetime(limit.getLimitDatetime())
                        .limitCurrencyShortname(limit.getLimitCurrencyShortname())
                        .expenseCategory(limit.getExpenseCategory())
                        .build())
                .toList();
    }

    @Description("Get all transactions of a client that have exceeded the monthly limit, including historical limit details")
    public List<ExceededTransactionResponseDto> getExceededTransactions(String account) {
        log.info("MCP Tool: getExceededTransactions called for account {}", account);
        return transactionService.getExceededTransactions(account);
    }
}