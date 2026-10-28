package com.xerxes.msme.dashboard.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardResponse(
        BigDecimal inventoryValue,
        BigDecimal salesToday,
        BigDecimal salesThisWeek,
        BigDecimal salesThisMonth,
        long totalProducts,
        long totalCustomers,
        long totalVendors,
        long lowStockCount,
        List<TopProductResponse> topProducts,
        List<RecentInvoiceResponse> recentInvoices) {}
