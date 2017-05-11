package cc.r2.core.poly.univar2;

import cc.r2.core.number.BigInteger;
import cc.r2.core.number.primes.SmallPrimes;
import cc.r2.core.poly.AbstractPolynomialTest;
import cc.r2.core.poly.ModularDomain;
import cc.r2.core.test.Benchmark;
import cc.r2.core.util.TimeUnits;
import org.apache.commons.math3.random.RandomDataGenerator;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.junit.Assert;
import org.junit.Test;

import static cc.r2.core.poly.univar2.Factorization.factorZ;
import static cc.r2.core.poly.univar2.Factorization.factorZp;
import static cc.r2.core.poly.univar2.FactorizationTestUtil.assertFactorization;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by poslavsky on 27/02/2017.
 */
public class FactorizationTest extends AbstractPolynomialTest {
    @Test
    public void test1() throws Exception {
        assertTrue(Factorization.factorZp(lMutablePolynomialZ.create(3, 7).modulus(19)).get(0).isMonic());
    }

    @Test
    public void test2() throws Exception {
        BigInteger modulus = BigInteger.LONG_MAX_VALUE;
        modulus = modulus.multiply(modulus).increment().nextProbablePrime();
        gMutablePolynomial<BigInteger> poly = gMutablePolynomial.create(new ModularDomain(modulus),
                BigInteger.valueOf(Long.MAX_VALUE),
                BigInteger.valueOf(Long.MAX_VALUE - 1),
                BigInteger.valueOf(Long.MAX_VALUE - 2));
        for (int i = 0; i < 5; i++)
            poly = poly.square().add(poly.derivative()).increment();
        FactorDecomposition<gMutablePolynomial<BigInteger>> fct = factorZp(poly);
        Assert.assertEquals(7, fct.size());
        assertFactorization(poly, fct);
    }

    @Test
    public void test3() throws Exception {
        long modulus = 13;
        lMutablePolynomialZp poly = lMutablePolynomialZ.create(5, 8, 1, 5, 7, 0, 0, 1, 5, 7, 0, 9, 3, 2).modulus(modulus);
        assertFactorization(poly, Factorization.factorZp(poly));
    }

    @Test
    public void test4_randomZp() throws Exception {
        RandomGenerator rnd = getRandom();
        RandomDataGenerator rndd = getRandomData();

        int nIterations = its(1000, 3000);
        for (int n = 0; n < nIterations; n++) {
            int nFactors = rndd.nextInt(4, 8);
            long modulus = getModulusRandom(rndd.nextInt(5, 31));
            lMutablePolynomialZp poly = lMutablePolynomialZp.constant(modulus, rndd.nextLong(1, modulus - 1));
            int expectedNFactors = 0;
            for (int i = 0; i < nFactors; i++) {
                lMutablePolynomialZp m = RandomPolynomials.randomMonicPoly(rndd.nextInt(1, 5), modulus, rnd);
                if (!m.isConstant()) ++expectedNFactors;
                if (m.isZero()) continue;
                poly = poly.multiply(m);
            }

            try {
                FactorDecomposition<lMutablePolynomialZp> lFactors = Factorization.factorZp(poly);
                assertTrue(lFactors.sumExponents() >= expectedNFactors);
                assertFactorization(poly, lFactors);

                if (n % 100 == 0) {
                    FactorDecomposition<gMutablePolynomial<BigInteger>> bFactors = factorZp(poly.toBigPoly());
                    FactorDecomposition<gMutablePolynomial<BigInteger>> converted = FactorDecomposition.convert(lFactors);
                    converted.canonicalForm();
                    bFactors.canonicalForm();
                    Assert.assertEquals(converted, bFactors);
                }
            } catch (Throwable e) {
                System.out.println(expectedNFactors);
                System.out.println(modulus);
                System.out.println(poly.toStringForCopy());
                throw e;
            }
        }
    }

    @Test
    public void test4_randomZp_a() throws Exception {
        long modulus = 59;
        lMutablePolynomialZp poly = lMutablePolynomialZ.create(46, 16, 1, 54, 16, 57, 22, 15, 31, 21).modulus(modulus);
        FactorDecomposition<lMutablePolynomialZp> fct = factorZp(poly);
        assertEquals(5, fct.size());
        assertEquals(6, fct.sumExponents());
    }

    @Test
    public void test5_randomZ() throws Exception {
        RandomGenerator rnd = getRandom();
        RandomDataGenerator rndd = getRandomData();

        int nIterations = (int) its(100, 1000);
        int maxDegree = 30;
        for (int n = 0; n < nIterations; n++) {
            gMutablePolynomial<BigInteger> poly = gMutablePolynomial.create(1);
            int expectedNFactors = 0;
            while (true) {
                gMutablePolynomial<BigInteger> m = RandomPolynomials.randomPoly(rndd.nextInt(1, 15), BigInteger.LONG_MAX_VALUE, rnd);
                if (m.isZero()) continue;
                if (!m.isConstant()) ++expectedNFactors;
                poly = poly.multiply(m);
                if (poly.degree() >= maxDegree)
                    break;
            }

            FactorDecomposition<gMutablePolynomial<BigInteger>> lFactors = Factorization.factorZ(poly);
            assertTrue(lFactors.size() >= expectedNFactors);
            assertFactorization(poly, lFactors);
        }
    }

    @Test
    @Benchmark(runAnyway = true)
    public void test6_referenceZ() throws Exception {
        lMutablePolynomialZ a = lMutablePolynomialZ.create(1, 11, 121, 1, 1, 1, 1, 123);
        lMutablePolynomialZ b = lMutablePolynomialZ.create(1, 1, 1, 3, 1, 1, 2, 3, 4, 5, 6);
        lMutablePolynomialZ poly = a.multiply(b).primitivePart();

        lMutablePolynomialZp polyMod = poly.modulus(SmallPrimes.nextPrime(2131243213));


        DescriptiveStatistics timingZ = new DescriptiveStatistics(), timingZp = new DescriptiveStatistics();
        int nIterations = its(100, 10000);
        for (int i = 0; i < nIterations; i++) {
            if (i == 1000) {timingZ.clear(); timingZp.clear();}

            long start;
            start = System.nanoTime();
            FactorDecomposition<lMutablePolynomialZp> factorsZp = Factorization.factorZp(polyMod);
            long timeZp = System.nanoTime() - start;
            timingZp.addValue(timeZp);
            assertFactorization(polyMod, factorsZp);
            assertTrue(factorsZp.size() >= 2);
            assertEquals(5, factorsZp.size());

            start = System.nanoTime();
            FactorDecomposition<lMutablePolynomialZ> factorsZ = Factorization.factorZ(poly);
            long timeZ = System.nanoTime() - start;
            timingZ.addValue(timeZ);
            assertFactorization(poly, factorsZ);
            assertTrue(factorsZ.size() >= 2);
            assertEquals(2, factorsZ.size());

            if (i > 9990) {
                System.out.println("Zp: " + TimeUnits.nanosecondsToString(timeZp));
                System.out.println("Z : " + TimeUnits.nanosecondsToString(timeZ));
            }
        }
        System.out.println((timingZ));
        System.out.println((timingZp));

//        -XX:+AggressiveOpts
//        DescriptiveStatistics:
//        n: 9000
//        min: 324us
//        max: 16ms
//        mean: 500us
//        std dev: 325us
//        median: 440us
//        skewness: 22ns
//        kurtosis: 855ns
//
//        DescriptiveStatistics:
//        n: 9000
//        min: 1373us
//        max: 39ms
//        mean: 1907us
//        std dev: 895us
//        median: 1719us
//        skewness: 20ns
//        kurtosis: 735ns
//
    }

    @Test
    @Benchmark(runAnyway = true)
    public void test7_referenceZb() throws Exception {
        gMutablePolynomial<BigInteger> a = gMutablePolynomial.create(1, 2, 3, 5, 3, 2, 1),
                b = gMutablePolynomial.create(1, 2, -12443241213L, 412312, 3, 2, 123423554351L),
                c = gMutablePolynomial.create(-1, -2, -12443241213L, 412312, 3, 2, 123423554351L),
                d = gMutablePolynomial.create(-1, -2, -12441213L, 412312, 3, 2, 1234235543L),
                e = gMutablePolynomial.create(-11111111, -2, -12441213L, 412312, 3, 2, 1234235543L),
                f = gMutablePolynomial.create(-11, -2222121, -12441213L, 412312, 3, 2, 1234235543L),
                g = gMutablePolynomial.create(-33, -2, -12441213L, 412312, 3, 2, 1234235543L, -12441213L, 412312, 3, 2, 1234235543L, -1, -2, -12441213L, 412312, 3, 2, 1234235543L, -12441213L, 412312, 3, 2, 1234235543L);
        gMutablePolynomial<BigInteger> poly = a.clone().multiply(b, c, d, e, f, g, g.clone().increment(), f.clone().increment());


        DescriptiveStatistics timing = new DescriptiveStatistics();
        int nIterations = its(100, 100);
        for (int i = 0; i < nIterations; i++) {
            if (i == 1000)
                timing.clear();
            long start = System.nanoTime();
            FactorDecomposition<gMutablePolynomial<BigInteger>> factors = Factorization.factorZ(poly);
            long time = System.nanoTime() - start;
            timing.addValue(time);
            assertEquals(9, factors.size());
            assertFactorization(poly, factors);
        }
        System.out.println(TimeUnits.statisticsNanotimeFull(timing));

        //    -XX:+AggressivrOpts
        //    DescriptiveStatistics:
        //    n: 100
        //    min: 165ms
        //    max: 1327ms
        //    mean: 326ms
        //    std dev: 139ms
        //    median: 300ms
        //    skewness: 4ns
        //    kurtosis: 26ns
    }
}