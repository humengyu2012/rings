package cc.r2.core.poly.univar2;


import cc.r2.core.number.BigInteger;
import cc.r2.core.number.BigIntegerArithmetics;
import cc.r2.core.number.ChineseRemainders;
import cc.r2.core.number.primes.PrimesIterator;
import cc.r2.core.poly.Domain;
import cc.r2.core.poly.IntegersDomain;
import cc.r2.core.poly.LongArithmetics;
import cc.r2.core.poly.ModularDomain;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;

import java.util.ArrayList;
import java.util.Arrays;

import static cc.r2.core.number.ChineseRemainders.ChineseRemainders;
import static cc.r2.core.poly.LongArithmetics.safeMultiply;
import static cc.r2.core.poly.LongArithmetics.safePow;
import static cc.r2.core.poly.univar2.DivisionWithRemainder.*;
import static cc.r2.core.poly.univar2.gMutablePolynomial.*;

/**
 * Polynomial GCD and sub-resultant sequence for univariate polynomials with single-precision coefficients.
 *
 * @author Stanislav Poslavsky
 * @since 1.0
 */
public final class PolynomialGCD {
    private PolynomialGCD() {}

    /**
     * Euclidean algorithm
     *
     * @param a poly
     * @param b poly
     * @return polynomial remainder sequence (the last element is GCD)
     */
    public static <T extends IMutablePolynomial<T>> PolynomialRemainders<T> Euclid(final T a, final T b) {
        if (a.degree() < b.degree())
            return Euclid(b, a);

        ArrayList<T> prs = new ArrayList<>();
        prs.add(a.clone()); prs.add(b.clone());

        if (a.isZero() || b.isZero()) return new PolynomialRemainders<>(prs);

        T x = a, y = b, r;
        while (true) {
            r = remainder(x, y, true);
            if (r == null)
                throw new IllegalArgumentException("Not divisible: (" + x + ") / (" + y + ")");

            if (r.isZero())
                break;
            prs.add(r);
            x = y;
            y = r;
        }
        return new PolynomialRemainders<>(prs);
    }

    /**
     * Runs extended Euclidean algorithm to compute {@code [gcd(a,b), x, y]} such that {@code x * a + y * b = gcd(a, b)}
     *
     * @param a the polynomial
     * @param b the polynomial
     * @return array of {@code [gcd(a,b), x, y]} such that {@code x * a + y * b = gcd(a, b)}
     */
    @SuppressWarnings("unchecked")
    public static <T extends IMutablePolynomial<T>> T[] ExtendedEuclid(final T a, final T b) {
        T s = a.createZero(), old_s = a.createOne();
        T t = a.createOne(), old_t = a.createZero();
        T r = b, old_r = a;

        T q;
        T tmp;
        while (!r.isZero()) {
            q = quotient(old_r, r, true);
            if (q == null)
                throw new IllegalArgumentException("Not divisible: (" + old_r + ") / (" + r + ")");

            tmp = old_r;
            old_r = r;
            r = tmp.clone().subtract(q.clone().multiply(r));

            tmp = old_s;
            old_s = s;
            s = tmp.clone().subtract(q.clone().multiply(s));

            tmp = old_t;
            old_t = t;
            t = tmp.clone().subtract(q.clone().multiply(t));
        }
        assert old_r.equals(a.clone().multiply(old_s).add(b.clone().multiply(old_t)));

        T[] result = a.arrayNewInstance(3);
        result[0] = old_r;
        result[1] = old_s;
        result[2] = old_t;
        return result;
    }

    /**
     * Euclidean algorithm for polynomials over Z that uses pseudo division
     *
     * @param a            poly
     * @param b            poly
     * @param primitivePRS whether to build primitive polynomial remainders or not
     * @return polynomial remainder sequence (the last element is GCD)
     */
    @SuppressWarnings("unchecked")
    //todo rename to PseudoEuclid
    public static <T extends IMutablePolynomial<T>> PolynomialRemainders<T> PolynomialEuclid(final T a,
                                                                                             final T b,
                                                                                             boolean primitivePRS) {
        if (a instanceof lMutablePolynomialZ)
            return (PolynomialRemainders<T>) PolynomialEuclid((lMutablePolynomialZ) a, (lMutablePolynomialZ) b, primitivePRS);
        else if (a instanceof gMutablePolynomial)
            return (PolynomialRemainders<T>) PolynomialEuclid((gMutablePolynomial) a, (gMutablePolynomial) b, primitivePRS);
        else
            throw new RuntimeException("Not a Z[x] polynomials: " + a.getClass());
    }

    /**
     * Euclidean algorithm for polynomials over Z that uses pseudo division
     *
     * @param a            poly
     * @param b            poly
     * @param primitivePRS whether to build primitive polynomial remainders or not
     * @return polynomial remainder sequence (the last element is GCD)
     */
    public static PolynomialRemainders<lMutablePolynomialZ> PolynomialEuclid(final lMutablePolynomialZ a,
                                                                             final lMutablePolynomialZ b,
                                                                             boolean primitivePRS) {
        if (a.degree < b.degree)
            return PolynomialEuclid(b, a, primitivePRS);


        if (a.isZero() || b.isZero()) return new PolynomialRemainders<>(a.clone(), b.clone());

        long aContent = a.content(), bContent = b.content();
        long contentGCD = LongArithmetics.gcd(aContent, bContent);
        lMutablePolynomialZ aPP = a.clone().divideOrNull(aContent), bPP = b.clone().divideOrNull(bContent);
        PolynomialRemainders<lMutablePolynomialZ> res = PolynomialEuclid0(aPP, bPP, primitivePRS);
        res.gcd().primitivePart().multiply(contentGCD);
        return res;
    }

    /**
     * Euclidean algorithm for polynomials over Z that uses pseudo division
     *
     * @param a            poly
     * @param b            poly
     * @param primitivePRS whether to build primitive polynomial remainders or not
     * @return polynomial remainder sequence (the last element is GCD)
     */
    @SuppressWarnings("unchecked")
    public static <E> PolynomialRemainders<gMutablePolynomial<E>> PolynomialEuclid(final gMutablePolynomial<E> a,
                                                                                   final gMutablePolynomial<E> b,
                                                                                   boolean primitivePRS) {
        if (a.degree < b.degree)
            return PolynomialEuclid(b, a, primitivePRS);

        if (a.isZero() || b.isZero())
            return new PolynomialRemainders<>(a.clone(), b.clone());

        E aContent = a.content(), bContent = b.content();
        E contentGCD = a.domain.gcd(aContent, bContent);
        gMutablePolynomial<E> aPP = a.clone().divideOrNull(aContent), bPP = b.clone().divideOrNull(bContent);
        PolynomialRemainders<gMutablePolynomial<E>> res = PolynomialEuclid0(aPP, bPP, primitivePRS);
        res.gcd().primitivePart().multiply(contentGCD);
        return res;
    }

    @SuppressWarnings("unchecked")
    private static <T extends IMutablePolynomial<T>> PolynomialRemainders<T> PolynomialEuclid0(final T aPP,
                                                                                               final T bPP,
                                                                                               boolean primitivePRS) {
        ArrayList<T> prs = new ArrayList<>();
        prs.add(aPP); prs.add(bPP);

        T x = aPP, y = bPP, r;
        while (true) {
            T[] tmp = DivisionWithRemainder.pseudoDivideAndRemainder(x, y, true);
            assert tmp != null && tmp[0] != null && tmp[1] != null;
            r = tmp[1];
            if (r.isZero())
                break;
            if (primitivePRS)
                r = r.primitivePart();
            prs.add(r);
            x = y;
            y = r;
        }
        return new PolynomialRemainders<>(prs);
    }

    /**
     * Euclidean algorithm for polynomials over Z that builds subresultants sequence
     *
     * @param a poly
     * @param b poly
     * @return subresultant sequence (the last element is GCD)
     */
    @SuppressWarnings("unchecked")
    public static <T extends IMutablePolynomial<T>> PolynomialRemainders<T> SubresultantEuclid(final T a,
                                                                                               final T b) {
        if (a instanceof lMutablePolynomialZ)
            return (PolynomialRemainders<T>) SubresultantEuclid((lMutablePolynomialZ) a, (lMutablePolynomialZ) b);
        else if (a instanceof gMutablePolynomial)
            return (PolynomialRemainders<T>) SubresultantEuclid((gMutablePolynomial) a, (gMutablePolynomial) b);
        else
            throw new RuntimeException("Not a Z[x] polynomials: " + a.getClass());
    }

    /**
     * Euclidean algorithm for polynomials over Z that builds subresultants sequence
     *
     * @param a poly
     * @param b poly
     * @return subresultant sequence (the last element is GCD)
     */
    public static PolynomialRemainders<lMutablePolynomialZ> SubresultantEuclid(final lMutablePolynomialZ a,
                                                                               final lMutablePolynomialZ b) {
        if (b.degree > a.degree)
            return SubresultantEuclid(b, a);

        if (a.isZero() || b.isZero()) return new PolynomialRemainders<>(a.clone(), b.clone());


        long aContent = a.content(), bContent = b.content();
        long contentGCD = LongArithmetics.gcd(aContent, bContent);
        lMutablePolynomialZ aPP = a.clone().divideOrNull(aContent), bPP = b.clone().divideOrNull(bContent);

        ArrayList<lMutablePolynomialZ> prs = new ArrayList<>();
        prs.add(aPP); prs.add(bPP);

        TLongArrayList beta = new TLongArrayList(), psi = new TLongArrayList();
        TIntArrayList deltas = new TIntArrayList();

        long cBeta, cPsi;
        for (int i = 0; ; i++) {
            lMutablePolynomialZ curr = prs.get(i);
            lMutablePolynomialZ next = prs.get(i + 1);
            int delta = curr.degree - next.degree;
            if (i == 0) {
                cBeta = (delta + 1) % 2 == 0 ? 1 : -1;
                cPsi = -1;
            } else {
                cPsi = safePow(-curr.lc(), deltas.get(i - 1));
                if (deltas.get(i - 1) < 1) {
                    cPsi = safeMultiply(cPsi, safePow(psi.get(i - 1), -deltas.get(i - 1) + 1));
                } else {
                    long tmp = safePow(psi.get(i - 1), deltas.get(i - 1) - 1);
                    assert cPsi % tmp == 0;
                    cPsi /= tmp;
                }
                cBeta = safeMultiply(-curr.lc(), safePow(cPsi, delta));
            }

            lMutablePolynomialZ q = DivisionWithRemainder.pseudoDivideAndRemainder(curr, next, true)[1];
            if (q.isZero())
                break;

            q = q.divideOrNull(cBeta);
            prs.add(q);

            deltas.add(delta);
            beta.add(cBeta);
            psi.add(cPsi);
        }
        PolynomialRemainders<lMutablePolynomialZ> res = new PolynomialRemainders<>(prs);
        res.gcd().multiply(contentGCD);
        return res;
    }

    /**
     * Euclidean algorithm for polynomials over Z that builds subresultants sequence
     *
     * @param a poly
     * @param b poly
     * @return subresultant sequence (the last element is GCD)
     */
    @SuppressWarnings("unchecked")
    public static <E> PolynomialRemainders<gMutablePolynomial<E>> SubresultantEuclid(final gMutablePolynomial<E> a,
                                                                                     final gMutablePolynomial<E> b) {
        if (b.degree > a.degree)
            return SubresultantEuclid(b, a);

        if (a.isZero() || b.isZero())
            return new PolynomialRemainders<>(a.clone(), b.clone());


        Domain<E> domain = a.domain;
        E aContent = a.content(), bContent = b.content();
        E contentGCD = domain.gcd(aContent, bContent);
        gMutablePolynomial<E> aPP = a.clone().divideOrNull(aContent), bPP = b.clone().divideOrNull(bContent);

        ArrayList<gMutablePolynomial<E>> prs = new ArrayList<>();
        prs.add(aPP); prs.add(bPP);

        ArrayList<E> beta = new ArrayList<>(), psi = new ArrayList<>();
        TIntArrayList deltas = new TIntArrayList();

        E cBeta, cPsi;
        for (int i = 0; ; i++) {
            gMutablePolynomial<E> curr = prs.get(i);
            gMutablePolynomial<E> next = prs.get(i + 1);
            int delta = curr.degree - next.degree;
            if (i == 0) {
                cBeta = (delta + 1) % 2 == 0 ? domain.getOne() : domain.getNegativeOne();
                cPsi = domain.getNegativeOne();
            } else {
                cPsi = domain.pow(domain.negate(curr.lc()), deltas.get(i - 1));
                if (deltas.get(i - 1) < 1) {
                    cPsi = domain.multiply(cPsi, domain.pow(psi.get(i - 1), -deltas.get(i - 1) + 1));
                } else {
                    E tmp = domain.pow(psi.get(i - 1), deltas.get(i - 1) - 1);
                    //assert cPsi.remainder(tmp).isZero();
                    cPsi = domain.divideExact(cPsi, tmp);
                }
                cBeta = domain.multiply(domain.negate(curr.lc()), domain.pow(cPsi, delta));
            }

            gMutablePolynomial<E> q = pseudoDivideAndRemainder(curr, next, true)[1];
            if (q.isZero())
                break;

            q = q.divideOrNull(cBeta);
            prs.add(q);

            deltas.add(delta);
            beta.add(cBeta);
            psi.add(cPsi);
        }
        PolynomialRemainders<gMutablePolynomial<E>> res = new PolynomialRemainders<>(prs);
        res.gcd().multiply(contentGCD);
        return res;
    }

    /**
     * Polynomial remainder sequence produced by the Euclidean algorithm
     */
    public static final class PolynomialRemainders<T extends IMutablePolynomial<T>> {
        /** actual data */
        public final ArrayList<T> remainders;

        @SuppressWarnings("unchecked")
        public PolynomialRemainders(T... remainders) {
            this(new ArrayList<>(Arrays.asList(remainders)));
        }

        public PolynomialRemainders(ArrayList<T> remainders) {
            this.remainders = remainders;
        }

        public T gcd() {
            if (remainders.size() == 2 && remainders.get(1).isZero())
                return remainders.get(0);
            return remainders.get(remainders.size() - 1);
        }
    }

    /**
     * Modular GCD algorithm for polynomials over Z.
     *
     * @param a the first polynomial
     * @param b the second polynomial
     * @return GCD of two polynomials
     */
    public static lMutablePolynomialZ ModularGCD(lMutablePolynomialZ a, lMutablePolynomialZ b) {
        if (a == b)
            return a.clone();
        if (a.isZero()) return b.clone();
        if (b.isZero()) return a.clone();

        if (a.degree < b.degree)
            return ModularGCD(b, a);
        long aContent = a.content(), bContent = b.content();
        long contentGCD = LongArithmetics.gcd(aContent, bContent);
        if (a.isConstant() || b.isConstant())
            return lMutablePolynomialZ.create(contentGCD);

        return ModularGCD0(a.clone().divideOrNull(aContent), b.clone().divideOrNull(bContent)).multiply(contentGCD);
    }

    /** modular GCD for primitive polynomials */
    @SuppressWarnings("ConstantConditions")
    private static lMutablePolynomialZ ModularGCD0(lMutablePolynomialZ a, lMutablePolynomialZ b) {
        assert a.degree >= b.degree;

        long lcGCD = LongArithmetics.gcd(a.lc(), b.lc());
        double bound = Math.max(a.mignotteBound(), b.mignotteBound()) * lcGCD;

        lMutablePolynomialZp previousBase, base = null;
        long basePrime = -1;

        PrimesIterator primesLoop = new PrimesIterator(3);
        while (true) {
            long prime = primesLoop.take();
            assert prime != -1 : "long overflow";

            if (a.lc() % prime == 0 || b.lc() % prime == 0)
                continue;

            lMutablePolynomialZp aMod = a.modulus(prime), bMod = b.modulus(prime);
            lMutablePolynomialZp modularGCD = Euclid(aMod, bMod).gcd();
            //clone if necessary
            if (modularGCD == aMod || modularGCD == bMod)
                modularGCD = modularGCD.clone();

            //coprime polynomials
            if (modularGCD.degree == 0)
                return lMutablePolynomialZ.one();

            //save the base
            if (base == null) {
                //make base monic and multiply lcGCD
                modularGCD.monic(lcGCD);
                base = modularGCD;
                basePrime = prime;
                continue;
            }

            //unlucky base => start over
            if (base.degree > modularGCD.degree) {
                base = null;
                basePrime = -1;
                continue;
            }

            //skip unlucky prime
            if (base.degree < modularGCD.degree)
                continue;

            //cache current base
            previousBase = base.clone();

            //lifting
            long newBasePrime = safeMultiply(basePrime, prime);
            long monicFactor = LongArithmetics.modInverse(modularGCD.lc(), prime);
            long lcMod = LongArithmetics.mod(lcGCD, prime);
            ChineseRemainders.ChineseRemaindersMagic magic = ChineseRemainders.createMagic(basePrime, prime);
            for (int i = 0; i <= base.degree; ++i) {
                //this is monic modularGCD multiplied by lcGCD mod prime
                //long oth = mod(safeMultiply(mod(safeMultiply(modularGCD.data[i], monicFactor), prime), lcMod), prime);

                long oth = modularGCD.multiply(modularGCD.multiply(modularGCD.data[i], monicFactor), lcMod);
                base.data[i] = ChineseRemainders(magic, base.data[i], oth);
            }
            base = base.setModulusUnsafe(newBasePrime);
            basePrime = newBasePrime;

            //either trigger Mignotte's bound or two trials didn't change the result, probably we are done
            if ((double) basePrime >= 2 * bound || base.equals(previousBase)) {
                lMutablePolynomialZ candidate = base.normalSymmetricForm().primitivePart();
                //first check b since b is less degree
                lMutablePolynomialZ[] div;
                div = DivisionWithRemainder.divideAndRemainder(b, candidate, true);
                if (div == null || !div[1].isZero())
                    continue;

                div = DivisionWithRemainder.divideAndRemainder(a, candidate, true);
                if (div == null || !div[1].isZero())
                    continue;

                return candidate;
            }
        }
    }

    /**
     * Modular GCD algorithm for polynomials over Z.
     *
     * @param a the first polynomial
     * @param b the second polynomial
     * @return GCD of two polynomials
     */
    @SuppressWarnings("ConstantConditions")
    public static gMutablePolynomial<BigInteger> ModularGCD(gMutablePolynomial<BigInteger> a, gMutablePolynomial<BigInteger> b) {
        if (a.domain != IntegersDomain.IntegersDomain)
            throw new IllegalArgumentException("Only polynomials over integers domain are allowed; " + a.domain);
        if (a == b)
            return a.clone();
        if (a.isZero()) return b.clone();
        if (b.isZero()) return a.clone();

        if (a.degree < b.degree)
            return ModularGCD(b, a);
        BigInteger aContent = a.content(), bContent = b.content();
        BigInteger contentGCD = BigIntegerArithmetics.gcd(aContent, bContent);
        if (a.isConstant() || b.isConstant())
            return a.createConstant(contentGCD);

        return ModularGCD0(a.clone().divideOrNull(aContent), b.clone().divideOrNull(bContent)).multiply(contentGCD);
    }

    /** modular GCD for primitive polynomials */
    @SuppressWarnings("ConstantConditions")
    private static gMutablePolynomial<BigInteger> ModularGCD0(gMutablePolynomial<BigInteger> a, gMutablePolynomial<BigInteger> b) {
        assert a.degree >= b.degree;

        BigInteger lcGCD = BigIntegerArithmetics.gcd(a.lc(), b.lc());
        BigInteger bound2 = BigIntegerArithmetics.max(mignotteBound(a), mignotteBound(b)).multiply(lcGCD).shiftLeft(1);
        if (bound2.isLong()
                && maxAbsCoefficient(a).isLong()
                && maxAbsCoefficient(b).isLong())
            return ModularGCD(asLongPolyZ(a), asLongPolyZ(b)).toBigPoly();

        lMutablePolynomialZp previousBase, base = null;
        long basePrime = -1;

        PrimesIterator primesLoop = new PrimesIterator(3);
        while (true) {
            long prime = primesLoop.take();
            assert prime != -1 : "long overflow";

            BigInteger bPrime = BigInteger.valueOf(prime);
            if (a.lc().remainder(bPrime).isZero() || b.lc().remainder(bPrime).isZero())
                continue;

            ModularDomain bPrimeDomain = new ModularDomain(bPrime);
            lMutablePolynomialZp aMod = asLongPolyZp(a.setDomain(bPrimeDomain)), bMod = asLongPolyZp(b.setDomain(bPrimeDomain));
            lMutablePolynomialZp modularGCD = Euclid(aMod, bMod).gcd();
            //clone if necessary
            if (modularGCD == aMod || modularGCD == bMod)
                modularGCD = modularGCD.clone();

            //coprime polynomials
            if (modularGCD.degree == 0)
                return a.createOne();

            //save the base
            if (base == null) {
                //make base monic and multiply lcGCD
                long lLcGCD = lcGCD.mod(bPrime).longValueExact();
                modularGCD.monic(lLcGCD);
                base = modularGCD;
                basePrime = prime;
                continue;
            }

            //unlucky base => start over
            if (base.degree > modularGCD.degree) {
                base = null;
                basePrime = -1;
                continue;
            }

            //skip unlucky prime
            if (base.degree < modularGCD.degree)
                continue;

            //cache current base
            previousBase = base.clone();

            if (!LongArithmetics.isOverflowMultiply(basePrime, prime) || basePrime * prime > LongArithmetics.MAX_SUPPORTED_MODULUS)
                break;

            //lifting
            long newBasePrime = safeMultiply(basePrime, prime);
            long monicFactor = LongArithmetics.modInverse(modularGCD.lc(), prime);
            long lcMod = lcGCD.mod(bPrime).longValueExact();
            ChineseRemainders.ChineseRemaindersMagic magic = ChineseRemainders.createMagic(basePrime, prime);
            for (int i = 0; i <= base.degree; ++i) {
                //this is monic modularGCD multiplied by lcGCD mod prime
                //long oth = mod(safeMultiply(mod(safeMultiply(modularGCD.data[i], monicFactor), prime), lcMod), prime);

                long oth = modularGCD.multiply(modularGCD.multiply(modularGCD.data[i], monicFactor), lcMod);
                base.data[i] = ChineseRemainders(magic, base.data[i], oth);
            }
            base = base.setModulusUnsafe(newBasePrime);
            basePrime = newBasePrime;

            //either trigger Mignotte's bound or two trials didn't change the result, probably we are done
            if (BigInteger.valueOf(basePrime).compareTo(bound2) >= 0 || base.equals(previousBase)) {
                gMutablePolynomial<BigInteger> candidate = base.normalSymmetricForm().primitivePart().toBigPoly();
                //first check b since b is less degree
                gMutablePolynomial<BigInteger>[] div;
                div = divideAndRemainder(b, candidate, true);
                if (div == null || !div[1].isZero())
                    continue;

                div = divideAndRemainder(a, candidate, true);
                if (div == null || !div[1].isZero())
                    continue;

                return candidate;
            }
        }

        //continue lifting with multi-precision integers
        gMutablePolynomial<BigInteger> bPreviousBase, bBase = base.toBigPoly();
        BigInteger bBasePrime = BigInteger.valueOf(basePrime);

        while (true) {
            long prime = primesLoop.take();
            assert prime != -1 : "long overflow";

            BigInteger bPrime = BigInteger.valueOf(prime);
            if (a.lc().remainder(bPrime).isZero() || b.lc().remainder(bPrime).isZero())
                continue;

            ModularDomain bPrimeDomain = new ModularDomain(bPrime);
            lMutablePolynomialZp aMod = asLongPolyZp(a.setDomain(bPrimeDomain)), bMod = asLongPolyZp(b.setDomain(bPrimeDomain));
            lMutablePolynomialZp modularGCD = Euclid(aMod, bMod).gcd();
            //clone if necessary
            if (modularGCD == aMod || modularGCD == bMod)
                modularGCD = modularGCD.clone();

            //coprime polynomials
            if (modularGCD.degree == 0)
                return a.createOne();

            //save the base
            if (bBase == null) {
                //make base monic and multiply lcGCD
                long lLcGCD = lcGCD.mod(bPrime).longValueExact();
                modularGCD.monic(lLcGCD);
                bBase = modularGCD.toBigPoly();
                bBasePrime = bPrime;
                continue;
            }

            //unlucky base => start over
            if (bBase.degree > modularGCD.degree) {
                bBase = null;
                bBasePrime = BigInteger.NEGATIVE_ONE;
                continue;
            }

            //skip unlucky prime
            if (bBase.degree < modularGCD.degree)
                continue;

            //cache current base
            bPreviousBase = bBase.clone();


            //lifting
            BigInteger newBasePrime = bBasePrime.multiply(bPrime);
            long monicFactor = LongArithmetics.modInverse(modularGCD.lc(), prime);
            long lcMod = lcGCD.mod(bPrime).longValueExact();
            for (int i = 0; i <= bBase.degree; ++i) {
                //this is monic modularGCD multiplied by lcGCD mod prime
                //long oth = mod(safeMultiply(mod(safeMultiply(modularGCD.data[i], monicFactor), prime), lcMod), prime);

                long oth = modularGCD.multiply(modularGCD.multiply(modularGCD.data[i], monicFactor), lcMod);
                bBase.data[i] = ChineseRemainders(bBasePrime, bPrime, bBase.data[i], BigInteger.valueOf(oth));
            }
            bBase = bBase.setDomainUnsafe(new ModularDomain(newBasePrime));
            bBasePrime = newBasePrime;

            //either trigger Mignotte's bound or two trials didn't change the result, probably we are done
            if (bBasePrime.compareTo(bound2) >= 0 || bBase.equals(bPreviousBase)) {
                gMutablePolynomial<BigInteger> candidate = asPolyZSymmetric(bBase).primitivePart();
                //first check b since b is less degree
                gMutablePolynomial<BigInteger>[] div;
                div = divideAndRemainder(b, candidate, true);
                if (div == null || !div[1].isZero())
                    continue;

                div = divideAndRemainder(a, candidate, true);
                if (div == null || !div[1].isZero())
                    continue;

                return candidate;
            }
        }
    }

    /**
     * Returns GCD of two polynomials. Modular GCD algorithm is used for Z[x], plain Euclid is used for Zp[x] and
     * subresultant used for other domains.
     *
     * @param a the first polynomial
     * @param b the second polynomial
     * @return GCD of two polynomials
     */
    @SuppressWarnings("unchecked")
    public static <T extends IMutablePolynomial<T>> T PolynomialGCD(T a, T b) {
        if (a instanceof lMutablePolynomialZp)
            return (T) Euclid((lMutablePolynomialZp) a, (lMutablePolynomialZp) b).gcd().monic();
        else if (a instanceof lMutablePolynomialZ)
            return (T) ModularGCD((lMutablePolynomialZ) a, (lMutablePolynomialZ) b);
        else if (a instanceof gMutablePolynomial) {
            Domain domain = ((gMutablePolynomial) a).domain;
            if (domain.isField())
                return (T) Euclid((gMutablePolynomial) a, (gMutablePolynomial) b).gcd().monic();
            else if (domain == IntegersDomain.IntegersDomain)
                return (T) ModularGCD((gMutablePolynomial) a, (gMutablePolynomial) b);
            else
                return (T) SubresultantEuclid((gMutablePolynomial) a, (gMutablePolynomial) b).gcd().monic();
        } else
            throw new RuntimeException(a.getClass().toString());
    }

    /**
     * Returns GCD of a list of polynomials.
     *
     * @param polynomials a set of polynomials
     * @return GCD of polynomials
     */
    public static <T extends IMutablePolynomial<T>> T PolynomialGCD(T... polynomials) {
        T gcd = polynomials[0];
        for (int i = 1; i < polynomials.length; i++)
            gcd = PolynomialGCD(gcd, polynomials[i]);
        return gcd;
    }

    /**
     * Returns GCD of a list of polynomials.
     *
     * @param polynomials a set of polynomials
     * @return GCD of polynomials
     */
    public static <T extends IMutablePolynomial<T>> T PolynomialGCD(Iterable<T> polynomials) {
        T gcd = null;
        for (T poly : polynomials)
            gcd = gcd == null ? poly : PolynomialGCD(gcd, poly);
        return gcd;
    }
}