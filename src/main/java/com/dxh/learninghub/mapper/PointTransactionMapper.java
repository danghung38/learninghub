package com.dxh.learninghub.mapper;

import com.dxh.learninghub.dto.response.PointTransactionResponse;
import com.dxh.learninghub.dto.response.PaymentSummaryResponse;
import com.dxh.learninghub.entity.Payment;
import com.dxh.learninghub.entity.PointTransaction;
import com.dxh.learninghub.enums.PointTransactionType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PointTransactionMapper {
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "user.fullName", target = "userFullName")
    @Mapping(source = "course.id", target = "courseId")
    @Mapping(source = "course.title", target = "courseTitle")
    @Mapping(source = "payment.id", target = "paymentId")
    @Mapping(target = "changedPoints", expression = "java(toSignedPoints(transaction))")
    PointTransactionResponse toResponse(PointTransaction transaction);

    @Mapping(source = "merchantTransactionRef", target = "transactionRef")
    PaymentSummaryResponse toPaymentSummary(Payment payment);

    default Long toSignedPoints(PointTransaction transaction) {
        if (transaction == null || transaction.getPoints() == null) {
            return null;
        }
        PointTransactionType type = transaction.getTransactionType();
        boolean debit = type == PointTransactionType.SPEND
                || type == PointTransactionType.WITHDRAW
                || type == PointTransactionType.ADMIN_DEBIT;
        return debit ? -transaction.getPoints() : transaction.getPoints();
    }
}
