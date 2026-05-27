package io.ituknown.result;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PaginationTest {

    @Test
    void pages_exactDivision() {
        Pagination p = new Pagination(20, 10, 1);
        assertEquals(2, p.getPages());
    }

    @Test
    void pages_hasRemainder() {
        Pagination p = new Pagination(21, 10, 1);
        assertEquals(3, p.getPages());
    }

    @Test
    void pages_singlePage() {
        Pagination p = new Pagination(5, 10, 1);
        assertEquals(1, p.getPages());
    }

    @Test
    void pages_zeroTotal() {
        Pagination p = new Pagination(0, 10, 1);
        assertEquals(0, p.getPages());
    }

    @Test
    void pages_pageSizeZero() {
        Pagination p = new Pagination(100, 0, 1);
        assertEquals(0, p.getPages());
    }

    @Test
    void pages_pageSizeNegative() {
        Pagination p = new Pagination(100, -1, 1);
        assertEquals(0, p.getPages());
    }

    @Test
    void getters() {
        Pagination p = new Pagination(100, 10, 3);
        assertEquals(100, p.getTotal());
        assertEquals(10, p.getPageSize());
        assertEquals(3, p.getCurrent());
        assertEquals(10, p.getPages());
    }
}
