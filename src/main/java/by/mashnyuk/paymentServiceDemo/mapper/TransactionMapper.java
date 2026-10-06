package by.mashnyuk.paymentServiceDemo.mapper;

import by.mashnyuk.paymentServiceDemo.model.Limit;
import by.mashnyuk.paymentServiceDemo.model.Transaction;
import by.mashnyuk.paymentServiceDemo.model.dto.request.TransactionRequestDto;
import by.mashnyuk.paymentServiceDemo.model.dto.response.ExceededTransactionResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sumUsd", ignore = true)
    @Mapping(target = "limitExceeded", ignore = true)
    Transaction toEntity(TransactionRequestDto dto);

    @Mapping(target = "accountFrom", source = "tx.accountFrom")
    @Mapping(target = "expenseCategory", source = "tx.expenseCategory")
    @Mapping(target = "limitSum", source = "limit.limitSum")
    @Mapping(target = "limitDatetime", source = "limit.limitDatetime")
    @Mapping(target = "limitCurrencyShortname", source = "limit.limitCurrencyShortname")
    ExceededTransactionResponseDto toExceededDto(Transaction tx, Limit limit);
}