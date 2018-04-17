package org.smartsoft.konfigurator.data

import org.smartsoft.konfigurator.Csp
import org.smartsoft.konfigurator.ExpFactory

class CarExample : ExpFactory() {

    val T2513 = +"T2513"
    val T2514 = +"T2514"
    val T2531 = +"T2531"
    val T2532 = +"T2532"
    val T2552 = +"T2552"
    val T2540 = +"T2540"
    val T2554 = +"T2554"
    val T2545 = +"T2545"
    val T2546 = +"T2546"
    val T2550 = +"T2550"
    val T2560 = +"T2560"

    val L4 = +"L4"
    val V6 = +"V6"
    val Hybrid = +"Hybrid"

    val Base = +"Base"
    val LE = +"LE"
    val SE = +"SE"
    val XLE = +"XLE"
    val Hyb = +"Hyb"

    val T6MT = +"T6MT"
    val T6AT = +"T6AT"
    val ECVT = +"ECVT"

    fun buildCsp(): Csp {

        xor(T2513, T2514, T2531, T2532, T2552, T2540, T2554, T2545, T2546, T2550, T2560)
        xor(L4, V6, Hybrid)
        xor(Base, LE, SE, XLE, Hyb)
        xor(T6MT, T6AT, ECVT)

        iff(T2513, and(Base, L4, T6MT))
        iff(T2514, and(Base, L4, T6AT))
        iff(T2531, and(LE, L4, T6MT))
        iff(T2532, and(LE, L4, T6AT))
        iff(T2552, and(LE, V6, T6AT))
        iff(T2545, and(SE, L4, T6MT))
        iff(T2546, and(SE, L4, T6AT))
        iff(T2550, and(SE, V6, T6AT))
        iff(T2554, and(XLE, V6, T6AT))
        iff(T2540, and(XLE, L4, T6AT))
        iff(T2560, and(Hyb, Hybrid, ECVT))

        return mkCsp()
    }

}