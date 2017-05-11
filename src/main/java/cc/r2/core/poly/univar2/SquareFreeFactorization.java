package cc.r2.core.poly.univar2;

import cc.r2.core.poly.LongArithmetics;

import java.util.Arrays;

import static cc.r2.core.poly.univar2.DivisionWithRemainder.divideAndRemainder;
import static cc.r2.core.poly.univar2.PolynomialGCD.PolynomialGCD;


/**
 * Square-free factorization of univariate polynomials over Z and Zp with single-precision coefficients.
 *
 * @author Stanislav Poslavsky
 * @since 1.0
 */
public final class SquareFreeFactorization {
    private SquareFreeFactorization() {}

    /**
     * Performs square-free factorization of a {@code poly} in Z[x].
     *
     * @param poly the polynomial
     * @return square-free decomposition
     */
    public static <Poly extends IMutablePolynomial<Poly>> FactorDecomposition<Poly> SquareFreeFactorizationZ(Poly poly) {
        if (poly.isConstant())
            return FactorDecomposition.constantFactor(poly);

        // x^2 + x^3 -> x^2 (1 + x)
        int exponent = 0;
        while (exponent <= poly.degree() && poly.isZeroAt(exponent)) { ++exponent; }
        if (exponent == 0)
            return SquareFreeFactorizationYun(poly);

        Poly expFree = poly.getRange(exponent, poly.degree() + 1);
        FactorDecomposition<Poly> fd = SquareFreeFactorizationYun(expFree);
        fd.addFactor(poly.createMonomial(exponent), 1);
        return fd;
    }

    /**
     * Performs square-free factorization of a poly using Yun's algorithm in Z[x].
     *
     * @param poly the polynomial
     * @return square-free decomposition
     */
    @SuppressWarnings("ConstantConditions")
    public static <Poly extends IMutablePolynomial<Poly>> FactorDecomposition<Poly> SquareFreeFactorizationYun(Poly poly) {
        if (poly.isConstant())
            return FactorDecomposition.constantFactor(poly);

        Poly content = poly.contentAsPoly();
        if (poly.signum() < 0)
            content = content.negate();

        poly = poly.clone().divideByLC(content);
        if (poly.degree() <= 1)
            return FactorDecomposition.oneFactor(content, poly);

        FactorDecomposition<Poly> factorization = FactorDecomposition.constantFactor(content);
        SquareFreeFactorizationYun(poly, factorization);
        return factorization;
    }

    private static <Poly extends IMutablePolynomial<Poly>> void SquareFreeFactorizationYun(Poly poly, FactorDecomposition<Poly> factorization) {
        Poly derivative = poly.derivative(), gcd = PolynomialGCD(poly, derivative);
        if (gcd.isConstant()) {
            factorization.addFactor(poly, 1);
            return;
        }

        Poly quot = divideAndRemainder(poly, gcd, false)[0], // safely destroy (cloned) poly (not used further)
                dQuot = divideAndRemainder(derivative, gcd, false)[0]; // safely destroy (cloned) derivative (not used further)

        int i = 0;
        while (!quot.isConstant()) {
            ++i;
            dQuot = dQuot.subtract(quot.derivative());
            Poly factor = PolynomialGCD(quot, dQuot);
            quot = divideAndRemainder(quot, factor, false)[0]; // can destroy quot in divideAndRemainder
            dQuot = divideAndRemainder(dQuot, factor, false)[0]; // can destroy dQuot in divideAndRemainder
            if (!factor.isOne())
                factorization.addFactor(factor, i);
        }
    }

    /**
     * Performs square-free factorization of a poly using Musser's algorithm in Z[x].
     *
     * @param poly the polynomial
     * @return square-free decomposition
     */
    @SuppressWarnings("ConstantConditions")
    public static <Poly extends IMutablePolynomial<Poly>> FactorDecomposition<Poly> SquareFreeFactorizationMusser(Poly poly) {
        if (poly.isConstant())
            return FactorDecomposition.constantFactor(poly);

        Poly content = poly.contentAsPoly();
        if (poly.signum() < 0)
            content = content.negate();

        poly = poly.clone().divideByLC(content);
        if (poly.degree() <= 1)
            return FactorDecomposition.oneFactor(content, poly);

        FactorDecomposition<Poly> factorization = FactorDecomposition.constantFactor(content);
        SquareFreeFactorizationMusser(poly, factorization);
        return factorization;
    }

    private static <Poly extends IMutablePolynomial<Poly>> void SquareFreeFactorizationMusser(Poly poly, FactorDecomposition<Poly> factorization) {
        Poly derivative = poly.derivative(), gcd = PolynomialGCD(poly, derivative);
        if (gcd.isConstant()) {
            factorization.addFactor(poly, 1);
            return;
        }

        Poly quot = divideAndRemainder(poly, gcd, false)[0]; // safely destroy (cloned) poly
        int i = 0;
        while (true) {
            ++i;
            Poly nextQuot = PolynomialGCD(gcd, quot);
            gcd = divideAndRemainder(gcd, nextQuot, false)[0]; // safely destroy gcd (reassigned)
            Poly factor = divideAndRemainder(quot, nextQuot, false)[0]; // safely destroy quot (reassigned further)
            if (!factor.isConstant())
                factorization.addFactor(factor, i);
            if (nextQuot.isConstant())
                break;
            quot = nextQuot;
        }
    }

    /**
     * Performs square-free factorization of a {@code poly} in Zp[x].
     *
     * @param poly the polynomial
     * @return square-free decomposition
     */
    public static <Poly extends IMutablePolynomial<Poly>> FactorDecomposition<Poly> SquareFreeFactorizationZp(Poly poly) {
        poly = poly.clone();
        Poly lc = poly.lcAsPoly();
        //make poly monic
        poly = poly.monic();

        if (poly.isConstant())
            return FactorDecomposition.constantFactor(lc);

        if (poly.degree() <= 1)
            return FactorDecomposition.oneFactor(lc, poly);

        FactorDecomposition<Poly> factorization;
        // x^2 + x^3 -> x^2 (1 + x)
        int exponent = 0;
        while (exponent <= poly.degree() && poly.isZeroAt(exponent)) { ++exponent; }
        if (exponent == 0)
            factorization = SquareFreeFactorizationMusser0(poly);
        else {
            Poly expFree = poly.getRange(exponent, poly.degree() + 1);
            factorization = SquareFreeFactorizationMusser0(expFree);
            factorization.addFactor(poly.createMonomial(exponent), 1);
        }

        return factorization.setConstantFactor(lc);
    }

    /** {@code poly} will be destroyed */
    @SuppressWarnings("ConstantConditions")
    private static <Poly extends IMutablePolynomial<Poly>> FactorDecomposition<Poly> SquareFreeFactorizationMusser0(Poly poly) {
        poly.monic();
        if (poly.isConstant())
            return FactorDecomposition.constantFactor(poly);

        if (poly.degree() <= 1)
            return FactorDecomposition.oneFactor(poly.createOne(), poly);

        Poly derivative = poly.derivative();
        if (!derivative.isZero()) {
            Poly gcd = PolynomialGCD(poly, derivative);
            if (gcd.isConstant())
                return FactorDecomposition.oneFactor(poly.createOne(), poly);
            Poly quot = divideAndRemainder(poly, gcd, false)[0]; // can safely destroy poly (not used further)

            FactorDecomposition<Poly> result = FactorDecomposition.constantFactor(poly.createOne());
            int i = 0;
            //if (!quot.isConstant())
            while (true) {
                ++i;
                Poly nextQuot = PolynomialGCD(gcd, quot);
                Poly factor = divideAndRemainder(quot, nextQuot, false)[0]; // can safely destroy quot (reassigned further)
                if (!factor.isConstant())
                    result.addFactor(factor.monic(), i);

                gcd = divideAndRemainder(gcd, nextQuot, false)[0]; // can safely destroy gcd
                if (nextQuot.isConstant())
                    break;
                quot = nextQuot;
            }
            if (!gcd.isConstant()) {
                gcd = pRoot(gcd);
                FactorDecomposition<Poly> gcdFactorization = SquareFreeFactorizationMusser0(gcd);
                gcdFactorization.raiseExponents(poly.domainCardinality().intValueExact());
                result.addAll(gcdFactorization);
                return result;
            } else
                return result;
        } else {
            Poly pRoot = pRoot(poly);
            FactorDecomposition<Poly> fd = SquareFreeFactorizationMusser0(pRoot);
            fd.raiseExponents(poly.domainCardinality().intValueExact());
            return fd.setConstantFactor(poly.createOne());
        }
    }

    /** p-th root of poly */
    @SuppressWarnings("unchecked")
    private static <Poly extends IMutablePolynomial<Poly>> Poly pRoot(Poly poly) {
        if (poly instanceof lMutablePolynomialZp)
            return (Poly) pRoot((lMutablePolynomialZp) poly);
        else if (poly instanceof gMutablePolynomial)
            return (Poly) pRoot((gMutablePolynomial) poly);
        else
            throw new RuntimeException(poly.getClass().toString());
    }

    /** p-th root of poly */
    private static lMutablePolynomialZp pRoot(lMutablePolynomialZp poly) {
        if (poly.domain.modulus > Integer.MAX_VALUE)
            throw new IllegalArgumentException("Too big modulus: " + poly.domain.modulus);
        int modulus = LongArithmetics.safeToInt(poly.domain.modulus);
        assert poly.degree % modulus == 0;
        long[] rootData = new long[poly.degree / modulus + 1];
        Arrays.fill(rootData, 0);
        for (int i = poly.degree; i >= 0; --i)
            if (poly.data[i] != 0) {
                assert i % modulus == 0;
                rootData[i / modulus] = poly.data[i];
            }
        return poly.createFromArray(rootData);
    }

    /** p-th root of poly */
    private static <E> gMutablePolynomial<E> pRoot(gMutablePolynomial<E> poly) {
        if (poly.domainCardinality() == null || !poly.domainCardinality().isInt())
            throw new IllegalArgumentException("Infinite or too large domain: " + poly.domain);

        int modulus = poly.domainCardinality().intValueExact();
        assert poly.degree % modulus == 0;
        E[] rootData = poly.domain.createZeroesArray(poly.degree / modulus + 1);
        Arrays.fill(rootData, 0);
        for (int i = poly.degree; i >= 0; --i)
            if (!poly.domain.isZero(poly.data[i])) {
                assert i % modulus == 0;
                rootData[i / modulus] = poly.data[i];
            }
        return poly.createFromArray(rootData);
    }

    /**
     * Returns {@code true} if {@code poly} is square-free and {@code false} otherwise
     *
     * @param poly the polynomial
     * @return {@code true} if {@code poly} is square-free and {@code false} otherwise
     */
    public static <T extends IMutablePolynomial<T>> boolean isSquareFree(T poly) {
        return PolynomialGCD(poly, poly.derivative()).isConstant();
    }

    /**
     * Performs square-free factorization of a {@code poly}.
     *
     * @param poly the polynomial
     * @return square-free decomposition
     */
    @SuppressWarnings("unchecked")
    public static <T extends IMutablePolynomial<T>> FactorDecomposition<T> SquareFreeFactorization(T poly) {
        if (poly.isOverField())
            return SquareFreeFactorizationZp(poly);
        else
            return SquareFreeFactorizationZ(poly);
    }

    /**
     * Returns square-free part of the {@code poly}
     *
     * @param poly the polynomial
     * @return square free part
     */
    public static <T extends IMutablePolynomial<T>> T SquareFreePart(T poly) {
        return SquareFreeFactorization(poly).factors.stream().filter(x -> !x.isMonomial()).reduce(poly.createOne(), IMutablePolynomial<T>::multiply);
    }
}