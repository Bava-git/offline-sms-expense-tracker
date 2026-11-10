package com.example.smsexpense.parser

import org.junit.Assert.*
import org.junit.Test

class BankSmsParserTest {
    private val p = BankSmsParserImplementation()

    @Test fun rejectOtp() { assertFalse(p.classify("Your OTP is 123456", "HDFCBK", 0).isBank) }
    @Test fun acceptDebit() { assertTrue(p.classify("Rs.450 debited from a/c XX1234 at SWIGGY", "HDFCBK", 0).isBank) }
    @Test fun extractAmount() { assertEquals(45000L, p.extractAmount("Rs.450.00")) }
    @Test fun direction() { assertEquals("DEBIT", p.extractDirection("debited")) }
    @Test fun extractUpiDetails() {
        val body = "Rs 500 paid to Rahul Kumar from a/c XX1234 via UPI Ref No: 123456789012"
        assertEquals("Rahul Kumar", p.extractCounterparty(body))
        assertEquals("1234", p.extractAccountLast4(body))
        assertEquals("123456789012", p.extractUpiReference(body))
    }

    @Test fun extractDebitCounterpartyAndAmountWithRsPeriod() {
        val body = "Rs.500.00 debited from a/c XX1234 to Rahul Kumar VPA rahul@upi"
        assertEquals(50000L, p.extractAmount(body))
        assertEquals("Rahul Kumar", p.extractCounterparty(body))
    }

    @Test fun extractAccountLast4FromAccountEndingFormat() {
        assertEquals("1234", p.extractAccountLast4("Your account ending in XX1234 was debited"))
    }

    @Test fun extractAccountLast4FromAcctLabel() {
        assertEquals("1234", p.extractAccountLast4("Rs.500 debited from Acct: XXXX1234"))
        assertEquals("5678", p.extractAccountLast4("Paid using Acct No. 12345678"))
    }

    @Test fun extractAccountSuffixFromThreeDigitMaskedAcct() {
        assertEquals("123", p.extractAccountLast4("Rs.500 debited from Acct XXX123"))
    }

    @Test fun parseIciciUpiDebitSmsFormat() {
        val body = "ICICI Bank Acct XX123 debited for Rs 1000.00 on 01-Nov-26; S MABOO JOHN credited. UPI:631104919700. Call 18002662 for dispute. SMS BLOCK 568 to 9215676766."
        assertTrue(p.classify(body, "ICICIB", 0).isBank)
        assertEquals("DEBIT", p.extractDirection(body))
        assertEquals(100000L, p.extractAmount(body))
        assertEquals("123", p.extractAccountLast4(body))
        assertEquals("S MABOO JOHN", p.extractCounterparty(body))
        assertEquals("631104919700", p.extractUpiReference(body))
    }

    @Test fun parseIciciDebitCounterpartyInTitleCase() {
        val body = "ICICI Bank Acct XX123 debited for Rs 92.00 on 03-Nov-26; S Sheik Mohamed credited. UPI:631181738371. Call 18002662 for dispute. SMS BLOCK 568 to 9215676766."
        assertEquals("S Sheik Mohamed", p.extractCounterparty(body))
    }
}
