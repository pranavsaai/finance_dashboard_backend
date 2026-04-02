package com.zorvyn.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class PageResponse<T> {

    private List<T> data;
    private int page;
    private int size;
    private long total;
}