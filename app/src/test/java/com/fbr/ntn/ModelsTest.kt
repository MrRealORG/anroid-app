package com.fbr.ntn

import com.fbr.ntn.model.LineItem
import com.fbr.ntn.model.PendingItem
import com.fbr.ntn.model.PendingStatus
import com.fbr.ntn.model.amountInWordsPKR
import com.fbr.ntn.model.money
import com.fbr.ntn.model.qtyFmt
import org.junit.Assert.assertEquals
import org.junit.Test

class ModelsTest {
    private val templateItems = listOf(
        LineItem("Paper Cone - 6 inch", "4822.1000", "PCS", 500.0, 200.0, 17.0),
        LineItem("Cotton Yarn - assorted count", "5206.0000", "KG", 100.0, 750.0, 18.0),
        LineItem("MS Steel Bar", "7214.9990", "KG", 250.0, 500.0, 18.0)
    )

    @Test fun lineTotals_matchTemplate() {
        val (a, b, c) = templateItems
        assertEquals(100000.0, a.valueExcl, 0.001)
        assertEquals(17000.0, a.tax, 0.001)
        assertEquals(117000.0, a.total, 0.001)
        assertEquals(13500.0, b.tax, 0.001)
        assertEquals(88500.0, b.total, 0.001)
        assertEquals(22500.0, c.tax, 0.001)
        assertEquals(147500.0, c.total, 0.001)
    }

    @Test fun invoiceTotals_matchTemplate() {
        val inv = PendingItem(
            "1", "INV-2026-000127", "Industrial Supplies", "ABC TEXTILE MILLS LTD.",
            "04 Sep 2026", "04 Oct 2026", PendingStatus.VALIDATE, "September 2026", "Due 04 Oct",
            items = templateItems
        )
        assertEquals(300000.0, inv.subtotal, 0.001)
        assertEquals(53000.0, inv.tax, 0.001)
        assertEquals(353000.0, inv.amount, 0.001)
    }

    @Test fun money_formats() {
        assertEquals("Rs 353,000.00", money(353000.0))
        assertEquals("Rs 1,150.00", money(1150.0))
    }

    @Test fun amountInWords_matchesTemplate() {
        assertEquals("Three hundred fifty-three thousand Pakistani rupees only", amountInWordsPKR(353000.0))
        assertEquals("Zero Pakistani rupees only", amountInWordsPKR(0.0))
    }

    @Test fun qtyFmt_stripsTrailingZero() {
        assertEquals("500", qtyFmt(500.0))
        assertEquals("17", qtyFmt(17.0))
    }
}
