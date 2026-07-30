package com.dxh.learninghub.mapper;

import com.dxh.learninghub.dto.response.admin.AdminWithdrawalResponse;
import com.dxh.learninghub.dto.response.WithdrawalResponse;
import com.dxh.learninghub.entity.Withdrawal;
import com.dxh.learninghub.service.AwsS3Service;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring", uses = AwsS3Service.class)
public interface WithdrawalMapper {

    @Mapping(source = "bankAccount.id", target = "bankAccountId")
    @Mapping(source = "accountNumber", target = "accountNumber", qualifiedByName = "maskAccountNumber")
    @Mapping(
            source = "paymentProofUrl",
            target = "paymentProofUrl",
            qualifiedByName = "resolveFileUrl")
    WithdrawalResponse toTeacherResponse(Withdrawal withdrawal);

    @Mapping(source = "teacher.id", target = "teacherId")
    @Mapping(source = "teacher.fullName", target = "teacherName")
    @Mapping(source = "bankAccount.id", target = "bankAccountId")
    @Mapping(
            source = "paymentProofUrl",
            target = "paymentProofUrl",
            qualifiedByName = "resolveFileUrl")
    AdminWithdrawalResponse toAdminResponse(Withdrawal withdrawal);

    @Named("maskAccountNumber")
    default String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() <= 4) {
            return accountNumber;
        }
        return "*".repeat(accountNumber.length() - 4)
                + accountNumber.substring(accountNumber.length() - 4);
    }
}
