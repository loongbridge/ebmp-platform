package com.ebmp.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {
    private List<T> content;
    private long total;
    private int page;
    private int size;

    public static <T> PageResult<T> of(Page<T> p) {
        return new PageResult<>(p.getContent(), p.getTotalElements(), p.getNumber() + 1, p.getSize());
    }
}
