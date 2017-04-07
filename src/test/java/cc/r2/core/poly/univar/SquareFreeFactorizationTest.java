package cc.r2.core.poly.univar;

import cc.r2.core.number.BigInteger;
import cc.r2.core.poly.AbstractPolynomialTest;
import cc.r2.core.poly.univar.FactorizationTestUtil.RandomSource;
import cc.r2.core.test.Benchmark;
import org.apache.commons.math3.random.RandomDataGenerator;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.junit.Assert;
import org.junit.Test;

import static cc.r2.core.poly.univar.FactorizationTestUtil.assertFactorization;
import static cc.r2.core.poly.univar.RandomPolynomials.randomPoly;
import static cc.r2.core.poly.univar.SquareFreeFactorization.SquareFreeFactorization;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by poslavsky on 20/01/2017.
 */
public class SquareFreeFactorizationTest extends AbstractPolynomialTest {
    @Test
    public void test1() throws Exception {
        lMutablePolynomialZ poly = PolynomialArithmetics.polyPow(lMutablePolynomialZ.create(1, 3).multiply(2), 3, false).multiply(PolynomialArithmetics.polyPow(lMutablePolynomialZ.create(-3, -5, 7), 2, false));
        assertFactorization(poly, SquareFreeFactorization.SquareFreeFactorizationYun(poly));
        poly = lMutablePolynomialZ.create(1, 3);
        assertFactorization(poly, SquareFreeFactorization.SquareFreeFactorizationYun(poly));
        poly = lMutablePolynomialZ.create(3);
        assertFactorization(poly, SquareFreeFactorization.SquareFreeFactorizationYun(poly));
        poly = lMutablePolynomialZ.create(33);
        assertFactorization(poly, SquareFreeFactorization.SquareFreeFactorizationYun(poly));
        poly = lMutablePolynomialZ.create(22, 22).multiply(lMutablePolynomialZ.create(12, 12, 12)).multiply(12);
        assertFactorization(poly, SquareFreeFactorization.SquareFreeFactorizationYun(poly));
    }

    @Test
    @Benchmark(runAnyway = true)
    public void testRandom2() throws Exception {
        RandomGenerator rnd = getRandom();
        RandomDataGenerator rndd = new RandomDataGenerator(rnd);
        DescriptiveStatistics yun = new DescriptiveStatistics(), musser = new DescriptiveStatistics();

        long overflows = 0;
        for (int i = 0; i < its(1000, 5000); i++) {
            if (i == 100) {
                yun.clear();
                musser.clear();
            }
            int nbase = rndd.nextInt(1, 5);
            lMutablePolynomialZ poly;
            try {
                poly = lMutablePolynomialZ.create(rndd.nextLong(1, 10));
                for (int j = 0; j < nbase; j++) {
                    lMutablePolynomialZ factor = RandomPolynomials.randomPoly(rndd.nextInt(1, 3), 10, rnd);
                    int exponent = rndd.nextInt(1, 5);
                    poly = poly.multiply(PolynomialArithmetics.polyPow(factor, exponent, true));
                }
            } catch (ArithmeticException e) {
                --i;
                continue;
            }
            try {
                long start = System.nanoTime();
                FactorDecomposition<lMutablePolynomialZ> yunFactorization = SquareFreeFactorization.SquareFreeFactorizationYun(poly);
                yun.addValue(System.nanoTime() - start);

                start = System.nanoTime();
                FactorDecomposition<lMutablePolynomialZ> musserFactorization = SquareFreeFactorization.SquareFreeFactorizationMusser(poly);
                musser.addValue(System.nanoTime() - start);

                assertEquals(yunFactorization.factors.size(), musserFactorization.factors.size());
                assertFactorization(poly, musserFactorization);
                assertFactorization(poly, yunFactorization);
            } catch (ArithmeticException exc) {
                if (!exc.getMessage().contains("overflow"))
                    throw exc;
                ++overflows;
            }
        }

        System.out.println("Overflows: " + overflows);
        System.out.println("Timings    Yun: " + yun.getMean());
        System.out.println("Timings Musser: " + musser.getMean());
    }

    @Test
    public void test3() throws Exception {
        lMutablePolynomialZ poly = lMutablePolynomialZ.create(0, 0, -1458, 6561, -6561);
        FactorDecomposition<lMutablePolynomialZ> factorization = SquareFreeFactorization.SquareFreeFactorizationYun(poly);
        assertFactorization(poly, factorization);
    }


    @Test
    public void test4() throws Exception {
        lMutablePolynomialZp poly = lMutablePolynomialZ.create(1, 0, 1, 0, 2).modulus(2);
        poly = poly.multiply(lMutablePolynomialZ.create(1, 0, 1, 0, 2).modulus(2));
        assertFactorization(poly, SquareFreeFactorization.SquareFreeFactorization(poly));
    }

    @Test
    public void test5() throws Exception {
        lMutablePolynomialZp poly = lMutablePolynomialZ.create(1, 0, 0, 0, 1).modulus(2);
        assertFactorization(poly, SquareFreeFactorization.SquareFreeFactorization(poly));
    }

    @Test
    @Benchmark(runAnyway = true)
    public void testRandom6() throws Exception {
        final class test {
            long overflows = 0;
            final DescriptiveStatistics arithmetics = new DescriptiveStatistics();
            final DescriptiveStatistics timings = new DescriptiveStatistics();

            void run(int bound, int maxDegree, int maxNBase, int maxExponent, int nIterations) {
                RandomGenerator rnd = getRandom();
                RandomDataGenerator rndd = new RandomDataGenerator(rnd);
                long start;
                for (int i = 0; i < nIterations; i++) {
                    if (i == 100)
                        timings.clear();
                    int nbase = rndd.nextInt(1, maxNBase);
                    for (long modulus : getModulusArray(9, 1, 35)) {
                        lMutablePolynomialZp poly;
                        start = System.nanoTime();
                        poly = lMutablePolynomialZ.create(rndd.nextLong(1, 1000)).modulus(modulus, false);
                        for (int j = 0; j < nbase; j++) {
                            lMutablePolynomialZp f = RandomPolynomials.randomPoly(rndd.nextInt(1, maxDegree), bound, rnd).modulus(modulus);
                            poly = poly.multiply(PolynomialArithmetics.polyPow(f, rndd.nextInt(1, maxExponent), true));
                        }
                        arithmetics.addValue(System.nanoTime() - start);
                        try {
                            start = System.nanoTime();
                            FactorDecomposition<lMutablePolynomialZp> factorization = SquareFreeFactorization.SquareFreeFactorization(poly);
                            timings.addValue(System.nanoTime() - start);
                            assertFactorization(poly, factorization);
                        } catch (ArithmeticException exc) {
                            if (!exc.getMessage().contains("overflow"))
                                throw exc;
                            ++overflows;
                        }

                    }
                }
            }
        }
        test smallPolys = new test();
        smallPolys.run(10, 3, 5, 5, (int) its(100, 1000));
        System.out.println("Overflows: " + smallPolys.overflows);
        System.out.println("Timings:\n" + smallPolys.timings.getPercentile(50));
        System.out.println("Arithmetics:\n" + smallPolys.arithmetics.getPercentile(50));

        System.out.println("\n================== \n");
        test largePolys = new test();
        largePolys.run(1000, 15, 10, 20, (int) its(10, 10));
        System.out.println("Overflows: " + largePolys.overflows);
        System.out.println("Timings:\n" + largePolys.timings.getPercentile(50));
        System.out.println("Arithmetics:\n" + largePolys.arithmetics.getPercentile(50));
    }

    @Test
    public void test6a() throws Exception {
        lMutablePolynomialZp poly = lMutablePolynomialZ.create(0, 0, 1, 3, 4, 3, 3, 2, 3).modulus(5);
        assertFactorization(poly, SquareFreeFactorization.SquareFreeFactorization(poly));
    }

    @Test
    public void test6b() throws Exception {
        lMutablePolynomialZp poly = lMutablePolynomialZ.create(0, 0, 0, 2).modulus(3);
        assertFactorization(poly, SquareFreeFactorization.SquareFreeFactorization(poly));
    }

    @Test
    public void test6c() throws Exception {
        lMutablePolynomialZp poly = lMutablePolynomialZ.create(0, 0, 0, 1, 1).modulus(2);
        assertFactorization(poly, SquareFreeFactorization.SquareFreeFactorization(poly));
    }

    @Test
    public void test6d() throws Exception {
        lMutablePolynomialZp poly = lMutablePolynomialZ.create(0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 2).modulus(3);
        assertFactorization(poly, SquareFreeFactorization.SquareFreeFactorization(poly));
    }

    @Test
    public void test6e() throws Exception {
        lMutablePolynomialZp poly = lMutablePolynomialZ.create(2, 3, 2, 1, 3, 3, 3).modulus(5);
        assertFactorization(poly, SquareFreeFactorization.SquareFreeFactorization(poly));
    }

    @Test
    public void test6f() throws Exception {
        lMutablePolynomialZp poly = lMutablePolynomialZ.create(1, 0, 2, 2, 0, 1, 1, 0, 2, 2, 0, 1).modulus(3);
        assertFactorization(poly, SquareFreeFactorization.SquareFreeFactorization(poly));
    }

    @Test
    public void test6g() throws Exception {
        for (long modulus : getModulusArray(1, 1, 50)) {
            lMutablePolynomialZp poly = lMutablePolynomialZ.create(0, 0, 0, 8, 20, 67, 55).modulus(modulus);
            assertFactorization(poly, SquareFreeFactorization.SquareFreeFactorization(poly));
        }
    }

    @Test
    public void test7() throws Exception {
        Assert.assertEquals(1, SquareFreeFactorization.SquareFreeFactorization(lMutablePolynomialZ.create(0, 0, 0, 0, 1)).factors.size());
        Assert.assertEquals(1, SquareFreeFactorization.SquareFreeFactorization(lMutablePolynomialZ.create(0, 0, 0, 0, 1).modulus(31)).factors.size());
    }

    @Test
    public void test8() throws Exception {
        RandomSource rnd = new RandomSource(getRandom(), 10, 20, false);
        long modulus = 3;
        for (int i = 0; i < 1000; i++) {
            lMutablePolynomialZp poly = rnd.take(modulus);
            assertTrue(SquareFreeFactorization.isSquareFree(SquareFreeFactorization.SquareFreePart(poly)));
        }
    }

    @Test
    public void test9() throws Exception {
        assertTrue(SquareFreeFactorization.SquareFreeFactorization(lMutablePolynomialZ.create(3, 7).modulus(17)).get(0).isMonic());
    }

    @Test
    public void test10() throws Exception {
        bMutablePolynomialZ poly = bMutablePolynomialZ.create(Long.MAX_VALUE, Long.MAX_VALUE - 1, Long.MAX_VALUE - 2, Long.MAX_VALUE - 3);
        bMutablePolynomialZ original = poly.add(poly.derivative());
        bMutablePolynomialZ squared = original.clone().square();

        bFactorDecomposition<bMutablePolynomialZ> f = SquareFreeFactorization(squared);
        assertEquals(1, f.size());
        assertEquals(2, f.getExponent(0));
        assertEquals(original, f.get(0));

        bMutablePolynomialZp squaredMod = squared.modulus(BigInteger.LONG_MAX_VALUE.multiply(BigInteger.TWO).nextProbablePrime());
        squaredMod = squaredMod.square();

        bFactorDecomposition<bMutablePolynomialZp> fMod = SquareFreeFactorization(squaredMod);
        assertEquals(1, fMod.size());
        assertEquals(4, fMod.getExponent(0));
        assertFactorization(squaredMod, fMod);
    }

    @Test
    public void testRandom11() throws Exception {
        RandomGenerator rnd = getRandom();
        RandomDataGenerator rndd = new RandomDataGenerator(rnd);
        BigInteger bound = BigInteger.LONG_MAX_VALUE;
        bound = bound.multiply(bound).increment();
        int nIterations = (int) its(20, 50);
        for (int i = 0; i < nIterations; i++) {
            bMutablePolynomialZ poly;
            poly = RandomPolynomials.randomPoly(rndd.nextInt(2, 30), bound, rnd);
            int exponent = rndd.nextInt(2, 4);
            poly = PolynomialArithmetics.polyPow(poly, exponent, false);

            bFactorDecomposition<bMutablePolynomialZ> zf = SquareFreeFactorization(poly);
            assertTrue(zf.sumExponents() >= exponent);
            assertFactorization(poly, zf);

            bMutablePolynomialZp polyMod = poly.modulus(RandomPolynomials.randomInt(bound, rnd).nextProbablePrime());
            bFactorDecomposition<bMutablePolynomialZp> zpf = SquareFreeFactorization(polyMod);
            assertTrue(zpf.sumExponents() >= exponent);
            assertFactorization(polyMod, zpf);
        }
    }
}