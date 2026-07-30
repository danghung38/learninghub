package com.dxh.learninghub.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;

public class GenderSubSetValidator implements ConstraintValidator<GenderSubset, String> {
    private String[] genderValues;

    @Override
    public void initialize(GenderSubset constraint) {
        // Lưu danh sách giá trị hợp lệ dưới dạng String
        genderValues = Arrays.stream(constraint.anyOf())
                .map(Enum::name) // Chuyển Enum thành String
                .toArray(String[]::new);
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return true; // Cho phép null
        return Arrays.stream(genderValues)
                .anyMatch(g -> g.equalsIgnoreCase(value)); // Kiểm tra giá trị
    }
}
