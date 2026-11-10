package com.example.smsexpense.util

object Money {
    fun paiseToRupees(paise: Long): String = "%.2f".format(paise / 100.0)
    fun rupeesToPaise(rupees: Double): Long = (rupees * 100).toLong()
}
