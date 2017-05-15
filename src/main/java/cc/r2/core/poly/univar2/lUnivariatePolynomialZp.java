package cc.r2.core.poly.univar2;

import cc.r2.core.number.BigInteger;
import cc.r2.core.poly.IntegersModulo;
import cc.r2.core.poly.LongArithmetics;
import cc.r2.core.poly.lIntegersModulo;

import java.util.Arrays;

/**
 * @author Stanislav Poslavsky
 * @since 1.0
 */
public final class lUnivariatePolynomialZp extends lUnivariatePolynomialAbstract<lUnivariatePolynomialZp> {
    /** the domain */
    public final lIntegersModulo domain;

    private lUnivariatePolynomialZp(lIntegersModulo domain, long[] data, int degree) {
        this.domain = domain;
        this.data = data;
        this.degree = degree;
    }

    private lUnivariatePolynomialZp(lIntegersModulo domain, long[] data) {
        this(domain, data, data.length - 1);
        fixDegree();
    }

    private static void checkModulus(long modulus) {
        if (Long.compare(modulus, LongArithmetics.MAX_SUPPORTED_MODULUS) > 0)
            throw new IllegalArgumentException("Too large modulus. Max allowed is " + LongArithmetics.MAX_SUPPORTED_MODULUS);
    }

    /**
     * Creates poly with specified coefficients represented as signed integers reducing them modulo {@code modulus}
     *
     * @param modulus the modulus
     * @param data    coefficients
     * @return the polynomial
     */
    public static lUnivariatePolynomialZp create(long modulus, long[] data) {
        lIntegersModulo domain = new lIntegersModulo(modulus);
        domain.modulus(data);
        return new lUnivariatePolynomialZp(domain, data);
    }

    /**
     * Creates linear polynomial of form {@code cc + x * lc}
     *
     * @param cc      the  constant coefficient
     * @param lc      the  leading coefficient
     * @param modulus the modulus
     * @return {@code cc + x * lc}
     */
    public static lUnivariatePolynomialZp linear(long cc, long lc, long modulus) {
        return create(modulus, new long[]{cc, lc});
    }

    public static lUnivariatePolynomialZp createUnsafe(long modulus, long[] data) {
        return new lUnivariatePolynomialZp(new lIntegersModulo(modulus), data);
    }

    public static lUnivariatePolynomialZp createUnsafe(lIntegersModulo domain, long[] data) {
        return new lUnivariatePolynomialZp(domain, data);
    }

    /**
     * Creates monomial {@code coefficient * x^exponent}
     *
     * @param modulus     the modulus
     * @param coefficient monomial coefficient
     * @param exponent    monomial exponent
     * @return {@code coefficient * x^exponent}
     */
    public static lUnivariatePolynomialZp createMonomial(long modulus, long coefficient, int exponent) {
        lIntegersModulo domain = new lIntegersModulo(modulus);
        coefficient = domain.modulus(coefficient);
        long[] data = new long[exponent + 1];
        data[exponent] = coefficient;
        return new lUnivariatePolynomialZp(domain, data);
    }

    /**
     * Creates constant polynomial with specified value
     *
     * @param modulus the modulus
     * @param value   the value
     * @return constant polynomial
     */
    public static lUnivariatePolynomialZp constant(long modulus, long value) {
        lIntegersModulo domain = new lIntegersModulo(modulus);
        return new lUnivariatePolynomialZp(domain, new long[]{domain.modulus(value)}, 0);
    }

    /**
     * Creates zero polynomial
     *
     * @param modulus the modulus
     * @return zero polynomial
     */
    public static lUnivariatePolynomialZp zero(long modulus) {
        return constant(modulus, 0L);
    }

    /**
     * Creates zero polynomial
     *
     * @param domain the domain
     * @return zero polynomial
     */
    public static lUnivariatePolynomialZp zero(lIntegersModulo domain) {
        return new lUnivariatePolynomialZp(domain, new long[]{0L}, 0);
    }

    /**
     * Creates unit polynomial
     *
     * @param modulus the modulus
     * @return 1
     */
    public static lUnivariatePolynomialZp one(long modulus) {
        return constant(modulus, 1L);
    }

    /**
     * Creates unit polynomial
     *
     * @param domain the domain
     * @return unit polynomial
     */
    public static lUnivariatePolynomialZp one(lIntegersModulo domain) {
        return new lUnivariatePolynomialZp(domain, new long[]{1L}, 0);
    }

    /** Returns the modulus */
    public long modulus() {
        return domain.modulus;
    }

    /** does not copy the data and does not reduce the data with new modulus */
    public lUnivariatePolynomialZp setModulusUnsafe(long newModulus) {
        return new lUnivariatePolynomialZp(new lIntegersModulo(newModulus), data, degree);
    }

    /**
     * Creates new Zp[x] polynomial with specified modulus.
     *
     * @param newModulus the new modulus
     * @return the new Zp[x] polynomial with specified modulus
     */
    public lUnivariatePolynomialZp setModulus(long newModulus) {
        long[] newData = data.clone();
        lIntegersModulo newDomain = new lIntegersModulo(newModulus);
        newDomain.modulus(newData);
        return new lUnivariatePolynomialZp(newDomain, newData);
    }

    /**
     * Returns Z[x] polynomial formed from the coefficients of this
     * represented in symmetric modular form ({@code -modulus/2 <= cfx <= modulus/2}).
     *
     * @return Z[x] version of this with coefficients represented in symmetric modular form ({@code -modulus/2 <= cfx <= modulus/2}).
     */
    @SuppressWarnings("unchecked")
    public lUnivariatePolynomialZ asPolyZSymmetric() {
        long[] newData = new long[degree + 1];
        for (int i = degree; i >= 0; --i)
            newData[i] = domain.symmetricForm(data[i]);
        return lUnivariatePolynomialZ.create(newData);
    }

    /**
     * Returns Z[x] polynomial formed from the coefficients of this.
     *
     * @param copy whether to copy the internal data
     * @return Z[x] version of this
     */
    @SuppressWarnings("unchecked")
    public lUnivariatePolynomialZ asPolyZ(boolean copy) {
        return lUnivariatePolynomialZ.create(copy ? data.clone() : data);
    }

    /** {@inheritDoc} */
    @Override
    public lUnivariatePolynomialZp[] arrayNewInstance(int length) {
        return new lUnivariatePolynomialZp[length];
    }

    /** {@inheritDoc} */
    @Override
    public lUnivariatePolynomialZp[] arrayNewInstance(lUnivariatePolynomialZp a, lUnivariatePolynomialZp b) {
        return new lUnivariatePolynomialZp[]{a, b};
    }

    /** {@inheritDoc} */
    @Override
    public lUnivariatePolynomialZp getRange(int from, int to) {
        return new lUnivariatePolynomialZp(domain, Arrays.copyOfRange(data, from, to));
    }

    /** {@inheritDoc} */
    @Override
    public boolean sameDomainWith(lUnivariatePolynomialZp oth) {
        return domain.modulus == oth.domain.modulus;
    }

    /** {@inheritDoc} */
    @Override
    public lUnivariatePolynomialZp createFromArray(long[] newData) {
        domain.modulus(newData);
        return new lUnivariatePolynomialZp(domain, newData);
    }

    /** {@inheritDoc} */
    @Override
    public lUnivariatePolynomialZp createMonomial(long coefficient, int newDegree) {
        long[] newData = new long[newDegree + 1];
        newData[newDegree] = valueOf(coefficient);
        return new lUnivariatePolynomialZp(domain, newData, newDegree);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isOverField() {return true;}

    /** {@inheritDoc} */
    @Override
    public boolean isOverFiniteField() {return true;}

    /** {@inheritDoc} */
    @Override
    public BigInteger coefficientDomainCardinality() {
        return BigInteger.valueOf(modulus());
    }

    /** {@inheritDoc} */
    @Override
    public BigInteger coefficientDomainCharacteristics() {
        return BigInteger.valueOf(modulus());
    }

    @Override
    long add(long a, long b) {
        return domain.add(a, b);
    }

    @Override
    long subtract(long a, long b) {
        return domain.subtract(a, b);
    }

    @Override
    long multiply(long a, long b) {
        return domain.multiply(a, b);
    }

    @Override
    long negate(long a) {
        return domain.negate(a);
    }

    @Override
    long valueOf(long a) {
        return domain.modulus(a);
    }

    /** {@inheritDoc} */
    @Override
    public lUnivariatePolynomialZp monic() {
        if (isMonic())
            return this;
        if (isZero())
            return this;
        if (degree == 0) {
            data[0] = 1;
            return this;
        }
        return multiply(domain.reciprocal(lc()));
    }

    /**
     * Sets {@code this} to its monic part multiplied by the {@code factor} modulo {@code modulus} (that is
     * {@code monic(modulus).multiply(factor)} ).
     *
     * @param factor the factor
     * @return {@code this}
     */
    public lUnivariatePolynomialZp monic(long factor) {
        return multiply(multiply(valueOf(factor), domain.reciprocal(lc())));
    }

    /** {@inheritDoc} */
    @Override
    public lUnivariatePolynomialZp divideByLC(lUnivariatePolynomialZp other) {
        return divide(other.lc());
    }

    /**
     * Divide by specified value
     *
     * @param val the value
     * @return {@code this / val}
     */
    public lUnivariatePolynomialZp divide(long val) {
        return multiply(domain.reciprocal(val));
    }

    /** {@inheritDoc} */
    @Override
    public lUnivariatePolynomialZp multiply(lUnivariatePolynomialZp oth) {
        if (isZero())
            return this;
        if (oth.isZero())
            return toZero();
        if (this == oth)
            return square();

        checkSameDomainWith(oth);
        if (oth.degree == 0)
            return multiply(oth.data[0]);
        if (degree == 0) {
            long factor = data[0];
            this.set(oth);
            return multiply(factor);
        }

        double rBound = normMax() * oth.normMax() * Math.max(degree + 1, oth.degree + 1);
        if (rBound < Long.MAX_VALUE) {
            // we can apply fast integer arithmetic and then reduce
            data = multiplyUnsafe0(oth);
            degree += oth.degree;
            domain.modulus(data);
            fixDegree();
        } else {
            data = multiplySafe0(oth);
            degree += oth.degree;
            fixDegree();
        }
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public lUnivariatePolynomialZp square() {
        if (isZero())
            return this;
        if (degree == 0)
            return multiply(data[0]);

        double norm1 = normMax();
        double rBound = norm1 * norm1 * (degree + 1);
        if (rBound < Long.MAX_VALUE) {
            // we can apply fast integer arithmetic and then reduce
            data = squareUnsafe0();
            degree += degree;
            domain.modulus(data);
            fixDegree();
        } else {
            data = squareSafe0();
            degree += degree;
            fixDegree();
        }
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public lUnivariatePolynomialZp derivative() {
        if (isConstant())
            return createZero();
        long[] newData = new long[degree];
        if (degree < domain.modulus)
            for (int i = degree; i > 0; --i)
                newData[i - 1] = multiply(data[i], i);
        else {
            int i = degree;
            for (; i >= domain.modulus; --i)
                newData[i - 1] = multiply(data[i], valueOf(i));
            for (; i > 0; --i)
                newData[i - 1] = multiply(data[i], i);
        }
        return new lUnivariatePolynomialZp(domain, newData);
    }

    /** {@inheritDoc} */
    @Override
    public UnivariatePolynomial<BigInteger> toBigPoly() {
        return UnivariatePolynomial.createUnsafe(new IntegersModulo(domain.modulus), dataToBigIntegers());
    }

    /** {@inheritDoc} */
    @Override
    public lUnivariatePolynomialZp clone() {
        return new lUnivariatePolynomialZp(domain, data.clone(), degree);
    }
}