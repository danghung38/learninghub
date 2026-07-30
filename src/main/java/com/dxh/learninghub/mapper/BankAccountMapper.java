package com.dxh.learninghub.mapper;

import com.dxh.learninghub.dto.request.CreateBankAccountRequest;
import com.dxh.learninghub.dto.request.UpdateBankAccountRequest;
import com.dxh.learninghub.dto.response.BankAccountResponse;
import com.dxh.learninghub.entity.BankAccount;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring")
public interface BankAccountMapper {

    @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
    BankAccount toEntity(CreateBankAccountRequest request);

    @BeanMapping(
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
            unmappedTargetPolicy = ReportingPolicy.IGNORE)
    void update(UpdateBankAccountRequest request, @MappingTarget BankAccount bankAccount);

    BankAccountResponse toResponse(BankAccount bankAccount);
}
