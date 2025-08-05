package core.ms.utils;

import core.ms.order.domain.value_objects.OrderStatusEnum;
import core.ms.shared.money.Currency;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public class EnumConverters {

    @Converter(autoApply = true)
    public static class OrderStatusEnumConverter implements AttributeConverter<OrderStatusEnum, String> {

        @Override
        public String convertToDatabaseColumn(OrderStatusEnum attribute) {
            return attribute != null ? attribute.name() : null;
        }

        @Override
        public OrderStatusEnum convertToEntityAttribute(String dbData) {
            return dbData != null ? OrderStatusEnum.valueOf(dbData) : null;
        }
    }

    @Converter(autoApply = true)
    public static class CurrencyConverter implements AttributeConverter<Currency, String> {

        @Override
        public String convertToDatabaseColumn(Currency attribute) {
            return attribute != null ? attribute.name() : null;
        }

        @Override
        public Currency convertToEntityAttribute(String dbData) {
            return dbData != null ? Currency.valueOf(dbData) : null;
        }
    }
}