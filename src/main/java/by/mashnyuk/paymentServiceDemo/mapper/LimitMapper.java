package by.mashnyuk.paymentServiceDemo.mapper;

import by.mashnyuk.paymentServiceDemo.model.Limit;
import by.mashnyuk.paymentServiceDemo.model.dto.response.LimitResponseDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LimitMapper {
    LimitResponseDto toDto(Limit limit);
}
