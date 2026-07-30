package com.dxh.learninghub.constant;

public interface AppConstant {
    String SEARCH_OPERATOR = "(\\w+?)(:|<|>)(.*)";
    String SORT_BY = "(\\w+?)(:)(.*)";
    String SEARCH_SPEC_OPERATOR = "([\\p{L}\\p{N}_]+?)([:><~!^$])([*]?)([^*]+)([*]?)";
}