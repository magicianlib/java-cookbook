package io.ituknown.result;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PageResultTest {

    // ========== getTotalPage ==========

    @Test
    void getTotalPage_exactDivision() {
        PageResult<String> result = new PageResult<>();
        result.setTotalCount(20);
        result.setPageSize(10);
        assertEquals(2, result.getTotalPage());
    }

    @Test
    void getTotalPage_hasRemainder() {
        PageResult<String> result = new PageResult<>();
        result.setTotalCount(21);
        result.setPageSize(10);
        assertEquals(3, result.getTotalPage());
    }

    @Test
    void getTotalPage_singlePage() {
        PageResult<String> result = new PageResult<>();
        result.setTotalCount(5);
        result.setPageSize(10);
        assertEquals(1, result.getTotalPage());
    }

    @Test
    void getTotalPage_zeroTotalCount() {
        PageResult<String> result = new PageResult<>();
        result.setTotalCount(0);
        result.setPageSize(10);
        assertEquals(0, result.getTotalPage());
    }

    @Test
    void getTotalPage_pageSizeZero() {
        PageResult<String> result = new PageResult<>();
        result.setTotalCount(100);
        result.setPageSize(0);
        assertEquals(0, result.getTotalPage());
    }

    @Test
    void getTotalPage_pageSizeNegative() {
        PageResult<String> result = new PageResult<>();
        result.setTotalCount(100);
        result.setPageSize(-1);
        assertEquals(0, result.getTotalPage());
    }

    @Test
    void getTotalPage_oneItemOnePageSize() {
        PageResult<String> result = new PageResult<>();
        result.setTotalCount(1);
        result.setPageSize(1);
        assertEquals(1, result.getTotalPage());
    }

    // ========== add ==========

    @Test
    void add_initializesWithArrayList() {
        PageResult<String> result = new PageResult<>();
        assertNull(result.getData());

        result.add("a");
        assertNotNull(result.getData());
        assertTrue(result.getData() instanceof ArrayList);
        assertEquals(1, result.getData().size());
        assertEquals("a", result.getData().iterator().next());
    }

    @Test
    void add_multipleItems() {
        PageResult<Integer> result = new PageResult<>();
        result.add(1);
        result.add(2);
        result.add(3);
        assertEquals(3, result.getData().size());
        assertEquals(List.of(1, 2, 3), new ArrayList<>(result.getData()));
    }

    @Test
    void add_withCustomCollection() {
        PageResult<String> result = new PageResult<>();
        result.add("a", LinkedList::new);
        assertTrue(result.getData() instanceof LinkedList);
    }

    // ========== addAll ==========

    @Test
    void addAll_initializesWithArrayList() {
        PageResult<Integer> result = new PageResult<>();
        result.addAll(List.of(1, 2, 3));
        assertEquals(3, result.getData().size());
    }

    @Test
    void addAll_appendsToExisting() {
        PageResult<Integer> result = new PageResult<>();
        result.add(0);
        result.addAll(List.of(1, 2));
        assertEquals(3, result.getData().size());
        assertEquals(List.of(0, 1, 2), new ArrayList<>(result.getData()));
    }

    @Test
    void addAll_withCustomCollection() {
        PageResult<String> result = new PageResult<>();
        result.addAll(List.of("x", "y"), LinkedList::new);
        assertTrue(result.getData() instanceof LinkedList);
        assertEquals(2, result.getData().size());
    }

    // ========== inheritance ==========

    @Test
    void extendsResult() {
        PageResult<String> result = new PageResult<>();
        assertInstanceOf(Result.class, result);
    }

    @Test
    void setterGetter() {
        PageResult<String> result = new PageResult<>();
        result.setPage(2);
        result.setPageSize(20);
        result.setTotalCount(100);
        result.setCode(0);
        result.setMessage("ok");
        result.setSuccess(true);

        assertEquals(2, result.getPage());
        assertEquals(20, result.getPageSize());
        assertEquals(100, result.getTotalCount());
        assertEquals(0, result.getCode());
        assertEquals("ok", result.getMessage());
        assertTrue(result.isSuccess());
    }
}
