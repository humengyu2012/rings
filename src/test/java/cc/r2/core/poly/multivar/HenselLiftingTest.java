package cc.r2.core.poly.multivar;

import cc.r2.core.combinatorics.IntCombinationsGenerator;
import cc.r2.core.number.BigInteger;
import cc.r2.core.number.primes.SmallPrimes;
import cc.r2.core.poly.IGeneralPolynomial;
import cc.r2.core.poly.IntegersModulo;
import cc.r2.core.poly.factorization.FactorizationTestData;
import cc.r2.core.poly.lIntegersModulo;
import cc.r2.core.poly.multivar.HenselLifting.Evaluation;
import cc.r2.core.poly.multivar.HenselLifting.IEvaluation;
import cc.r2.core.poly.multivar.HenselLifting.lEvaluation;
import cc.r2.core.poly.multivar.MultivariateFactorization.IEvaluationLoop;
import cc.r2.core.poly.multivar.MultivariateFactorization.lEvaluationLoop;
import cc.r2.core.test.Benchmark;
import cc.r2.core.util.TimeUnits;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import static cc.r2.core.poly.multivar.MultivariateFactorizationTest.*;
import static cc.r2.core.poly.multivar.lMultivariatePolynomialZp.parse;
import static cc.r2.core.test.AbstractTest.its;

/**
 * @author Stanislav Poslavsky
 * @since 1.0
 */
public class HenselLiftingTest {

    @Test
    public void test1() throws Exception {
        lIntegersModulo domain = new lIntegersModulo(SmallPrimes.nextPrime(66));
        String[] vars = {"a", "b"};
        lMultivariatePolynomialZp
                factors[] =
                {
                        parse("a^15*b + a^15 - 2*a*b^4 - 3*b + 2 + b^2*a - b^4", domain, vars),
                        parse("a^5*b^6 + a*b^2 - 3*b^2 + b + 2 - a^3*b^6", domain, vars)
                },
                base = multiply(factors);

        assert MultivariateGCD.PolynomialGCD(factors).isConstant();

        lEvaluation evaluation = new lEvaluation(base.nVariables, new long[]{1}, domain, base.ordering);

        lMultivariatePolynomialZp[] uFactors = evaluation.evaluateFrom(factors, 1);
        lMultivariatePolynomialZp[] factorsLC = Arrays.stream(factors).map(f -> f.lc(0)).toArray(lMultivariatePolynomialZp[]::new);
        HenselLifting.multivariateLift0(base,
                uFactors,
                factorsLC,
                evaluation,
                base.degrees());
        Assert.assertEquals(base, multiply(uFactors));
    }

    @Test
    public void test2() throws Exception {
        lIntegersModulo domain = new lIntegersModulo(43313);
        String[] vars = {"a", "b"};
        lMultivariatePolynomialZp
                factors[] =
                {
                        parse("36045*b^2+23621*a*b+21517*a^2", domain, vars),
                        parse("12894*b^3+22166*a+31033*a*b^2+25906*a^2*b^3", domain, vars),
                        parse("2387*b+11677*a+14775*a^2+25925*a^2*b", domain, vars),
                        parse("1+17708*a*b^2+7251*a*b^5+12898*a^2*b^2+12277*a^3*b^4+23269*a^5*b", domain, vars),
                        parse("27799+34918*a+25070*a^2+2145*a^2*b", domain, vars),
                },
                base = multiply(factors);

        assert MultivariateGCD.PolynomialGCD(factors).isConstant();

        lEvaluation evaluation = new lEvaluation(base.nVariables, new long[]{1146}, domain, base.ordering);

        lMultivariatePolynomialZp[] uFactors = evaluation.evaluateFrom(factors, 1);
        System.out.println(allCoprime(uFactors));
        System.out.println(IntStream.range(0, uFactors.length).allMatch(i -> factors[i].degree(0) == uFactors[i].degree(0)));

        lMultivariatePolynomialZp[] factorsLC = Arrays.stream(factors).map(f -> f.lc(0)).toArray(lMultivariatePolynomialZp[]::new);
        HenselLifting.multivariateLift0(base,
                uFactors,
                factorsLC,
                evaluation,
                base.degrees());
        Assert.assertEquals(base, multiply(uFactors));
    }


    @Test
    public void testEvaluation1() throws Exception {
        lIntegersModulo domain = new lIntegersModulo(97);
        String[] vars = {"a", "b", "c"};
        lMultivariatePolynomialZp
                poly = parse("b*a^2 + b + a^2 + 2 + a^3*b^4 - a^62*b + 3*b^55*a^55 + b^66 + 3*c^55 + c*a*b + 3", domain, vars);
        lEvaluation evaluation = new lEvaluation(poly.nVariables, new long[]{2, 3}, domain, poly.ordering);

        Assert.assertEquals(evaluation.evaluate(poly, 1), evaluation.modImage(poly, 1, 1));
        Assert.assertEquals(parse("53 + a^2 + 49*a^3 + 22*a^55 + 2*b + a^2*b + 32*a^3*b + 84*a^55*b + 96*a^62*b + a*b*c + 3*c^55", domain, vars),
                evaluation.modImage(poly, 1, 2));
        Assert.assertEquals(parse("31 + a^2 + 26*a^55 + 77*b + a^2*b + 93*a^55*b + 96*a^62*b + 96*b^2 + 60*a^55*b^2 + 52*b^3 + 4*a^55*b^3 + 6*b^4 + a^3*b^4 + 27*a^55*b^4 + 15*b^5 + 88*a^55*b^5 + 25*b^6 + 75*a^55*b^6 + a*b*c + 3*c^55", domain, vars),
                evaluation.modImage(poly, 1, 7));
        Assert.assertEquals(parse("89 + a^2 + 22*a^55 + 33*b + a^2*b + 81*a^55*b + 96*a^62*b + 83*b^2 + 26*a^55*b^2 + 54*b^3 + 32*a^55*b^3 + 52*b^4 + a^3*b^4 + 89*a^55*b^4 + 14*b^5 + 56*a^55*b^5 + 19*b^6 + 8*a^55*b^6 + 58*b^7 + 9*a^55*b^7 + 32*b^8 + 23*a^55*b^8 + 57*b^9 + 42*a^55*b^9 + 58*b^10 + 90*a^55*b^10 + 56*b^11 + 53*a^55*b^11 + 24*b^12 + 4*a^55*b^12 + 86*b^13 + 92*a^55*b^13 + 33*b^14 + 64*a^55*b^14 + 71*b^15 + 71*a^55*b^15 + 44*b^16 + 42*a^55*b^16 + 49*b^17 + 92*a^55*b^17 + 26*b^18 + 94*a^55*b^18 + 77*b^19 + 78*a^55*b^19 + 76*b^20 + 24*a^55*b^20 + 37*b^21 + 76*a^55*b^21 + 3*b^22 + 64*a^55*b^22 + 80*b^23 + 71*a^55*b^23 + 77*b^24 + 89*a^55*b^24 + 71*b^25 + 7*a^55*b^25 + 92*b^26 + 87*a^55*b^26 + 65*b^27 + 49*a^55*b^27 + 25*b^28 + 60*a^55*b^28 + 30*b^29 + 19*a^55*b^29 + 95*b^30 + 16*a^55*b^30 + 32*b^31 + 3*a^55*b^31 + 85*b^32 + 62*a^55*b^32 + 24*b^33 + 91*a^55*b^33 + 32*b^34 + 55*a^55*b^34 + 79*b^35 + 58*a^55*b^35 + 57*b^36 + 82*a^55*b^36 + 30*b^37 + 62*a^55*b^37 + 44*b^38 + 64*a^55*b^38 + 61*b^39 + 95*a^55*b^39 + 68*b^40 + 17*a^55*b^40 + 57*b^41 + 35*a^55*b^41 + 76*b^42 + 96*a^55*b^42 + 2*b^43 + 29*a^55*b^43 + 83*b^44 + 37*a^55*b^44 + 42*b^45 + 92*a^55*b^45 + 3*b^46 + 42*a^55*b^46 + a*b*c + 3*c^55", domain, vars),
                evaluation.modImage(poly, 1, 47));


        Assert.assertEquals(evaluation.evaluate(poly, 2), evaluation.modImage(poly, 2, 1));
        Assert.assertEquals(parse("52 + a^2 + b + a^2*b + 96*a^62*b + a^3*b^4 + 3*a^55*b^55 + b^66 + 5*c + a*b*c", domain, vars),
                evaluation.modImage(poly, 2, 2));
        Assert.assertEquals(parse("87 + a^2 + b + a^2*b + 96*a^62*b + a^3*b^4 + 3*a^55*b^55 + b^66 + 9*c + a*b*c + 7*c^2 + 93*c^3 + 79*c^4 + 4*c^5 + 64*c^6", domain, vars),
                evaluation.modImage(poly, 2, 7));
        Assert.assertEquals(parse("52 + a^2 + b + a^2*b + 96*a^62*b + a^3*b^4 + 3*a^55*b^55 + b^66 + 36*c + a*b*c + 58*c^2 + 65*c^3 + 70*c^4 + 29*c^5 + 12*c^6 + 9*c^7 + 80*c^8 + 51*c^9 + 59*c^10 + 44*c^11 + 62*c^12 + 13*c^13 + 96*c^14 + 71*c^15 + 28*c^16 + 84*c^17 + 53*c^18 + 19*c^19 + 81*c^20 + 74*c^21 + 96*c^22 + 71*c^23 + 27*c^24 + 57*c^25 + 15*c^26 + 48*c^27 + 57*c^28 + 67*c^29 + 24*c^30 + 3*c^31 + 9*c^32 + 62*c^33 + 63*c^34 + 39*c^35 + 10*c^36 + 91*c^37 + 96*c^38 + 95*c^39 + 76*c^40 + 91*c^41 + 50*c^42 + 68*c^43 + 40*c^44 + 13*c^45 + 63*c^46", domain, vars),
                evaluation.modImage(poly, 2, 47));
    }

    @Test
    public void testEvaluation2() throws Exception {
        // small characteristics
        lIntegersModulo domain = new lIntegersModulo(2);
        String[] vars = {"a", "b", "c"};
        lMultivariatePolynomialZp
                poly = parse("b*a^2 + b + a^2 + 2 + a^3*b^4 - a^62*b + 3*b^55*a^55 + b^66 + 3*c^55 + c*a*b + 3", domain, vars);
        lEvaluation evaluation = new lEvaluation(poly.nVariables, new long[]{1, 3}, domain, poly.ordering);

        Assert.assertEquals(evaluation.evaluate(poly, 1), evaluation.modImage(poly, 1, 1));
        Assert.assertEquals(parse("a^2 + a^3 + b + a^2*b + a^55*b + a^62*b + a*b*c + c^55", domain, vars),
                evaluation.modImage(poly, 1, 2));
        Assert.assertEquals(parse("1 + a^2 + a^55 + b + a^2*b + a^55*b + a^62*b + b^2 + a^55*b^2 + a^55*b^3 + a^3*b^4 + a^55*b^4 + a^55*b^5 + a^55*b^6 + a*b*c + c^55", domain, vars),
                evaluation.modImage(poly, 1, 7));
        Assert.assertEquals(parse("1 + a^2 + b + a^2*b + a^62*b + b^2 + a^3*b^4 + a^55*b^7 + a^55*b^23 + a^55*b^39 + a*b*c + c^55", domain, vars),
                evaluation.modImage(poly, 1, 47));


        Assert.assertEquals(evaluation.evaluate(poly, 2), evaluation.modImage(poly, 2, 1));
        Assert.assertEquals(parse("1 + a^2 + b + a^2*b + a^62*b + a^3*b^4 + a^55*b^55 + b^66 + c + a*b*c", domain, vars),
                evaluation.modImage(poly, 2, 2));
        Assert.assertEquals(parse("a^2 + b + a^2*b + a^62*b + a^3*b^4 + a^55*b^55 + b^66 + c + a*b*c + c^2 + c^3 + c^4 + c^5 + c^6", domain, vars),
                evaluation.modImage(poly, 2, 7));
        Assert.assertEquals(parse("1 + a^2 + b + a^2*b + a^62*b + a^3*b^4 + a^55*b^55 + b^66 + a*b*c + c^7 + c^23 + c^39", domain, vars),
                evaluation.modImage(poly, 2, 47));
    }

    @Test
    public void testEvaluation3() throws Exception {
        // small characteristics
        IntegersModulo domain = new IntegersModulo(2);
        String[] vars = {"a", "b", "c"};
        MultivariatePolynomial<BigInteger>
                poly = MultivariatePolynomial.parse("b*a^2 + b + a^2 + 2 + a^3*b^4 - a^62*b + 3*b^55*a^55 + b^66 + 3*c^55 + c*a*b + 3", domain, vars);
        Evaluation<BigInteger> evaluation = new Evaluation<>(poly.nVariables, new BigInteger[]{BigInteger.ONE, BigInteger.ONE}, domain, poly.ordering);

        Assert.assertEquals(evaluation.evaluate(poly, 1), evaluation.modImage(poly, 1, 1));
        Assert.assertEquals(MultivariatePolynomial.parse("a^2 + a^3 + b + a^2*b + a^55*b + a^62*b + a*b*c + c^55", domain, vars),
                evaluation.modImage(poly, 1, 2));
        Assert.assertEquals(MultivariatePolynomial.parse("1 + a^2 + a^55 + b + a^2*b + a^55*b + a^62*b + b^2 + a^55*b^2 + a^55*b^3 + a^3*b^4 + a^55*b^4 + a^55*b^5 + a^55*b^6 + a*b*c + c^55", domain, vars),
                evaluation.modImage(poly, 1, 7));
        Assert.assertEquals(MultivariatePolynomial.parse("1 + a^2 + b + a^2*b + a^62*b + b^2 + a^3*b^4 + a^55*b^7 + a^55*b^23 + a^55*b^39 + a*b*c + c^55", domain, vars),
                evaluation.modImage(poly, 1, 47));


        Assert.assertEquals(evaluation.evaluate(poly, 2), evaluation.modImage(poly, 2, 1));
        Assert.assertEquals(MultivariatePolynomial.parse("1 + a^2 + b + a^2*b + a^62*b + a^3*b^4 + a^55*b^55 + b^66 + c + a*b*c", domain, vars),
                evaluation.modImage(poly, 2, 2));
        Assert.assertEquals(MultivariatePolynomial.parse("a^2 + b + a^2*b + a^62*b + a^3*b^4 + a^55*b^55 + b^66 + c + a*b*c + c^2 + c^3 + c^4 + c^5 + c^6", domain, vars),
                evaluation.modImage(poly, 2, 7));
        Assert.assertEquals(MultivariatePolynomial.parse("1 + a^2 + b + a^2*b + a^62*b + a^3*b^4 + a^55*b^55 + b^66 + a*b*c + c^7 + c^23 + c^39", domain, vars),
                evaluation.modImage(poly, 2, 47));
    }

    @Test
    public void testHenselLiftingRandom1() throws Exception {
        MultivariateFactorizationTest.lSampleDecompositionSource baseSource = new MultivariateFactorizationTest.lSampleDecompositionSource(
                3, 5,
                2, 4,
                2, 6,
                1, 4);
        baseSource.minModulusBits = 15;
        baseSource.maxModulusBits = 30;
        FactorizationTestData.SampleDecompositionSource<lMultivariatePolynomialZp> source
                = orderVarsByDegree(
                filterMonomialContent(
                        filterNonSquareFree(baseSource)));
        testHenselLift(source, its(100, 1000), lEvaluationLoop::new, HenselLifting::multivariateLift0, true, 2);
        testHenselLift(source, its(100, 1000), lEvaluationLoop::new, HenselLifting::multivariateLift0, true, 1);
    }

    @Benchmark(runAnyway = true)
    @Test
    public void testBivariateLifting1() throws Exception {
        lIntegersModulo domain = new lIntegersModulo(67);
        String[] vars = {"a", "b"};
        lMultivariatePolynomialZp
                base = parse("33*b+34*b^2+36*b^3+59*b^4+50*b^5+52*b^6+66*b^7+17*b^8+33*a^4+34*a^4*b+36*a^4*b^2+28*a^4*b^3+14*a^4*b^4+6*a^4*b^5+57*a^4*b^6+17*a^4*b^7+52*a^4*b^8+66*a^4*b^9+17*a^4*b^10+11*a^5*b+59*a^5*b^2+50*a^5*b^3+53*a^5*b^4+65*a^5*b^5+45*a^5*b^6+56*a^5*b^7+53*a^5*b^8+a^5*b^9+66*a^5*b^10+17*a^5*b^11+33*a^6*b^2+11*a^6*b^4+3*a^6*b^5+a^6*b^7+3*a^8*b^2+64*a^8*b^3+52*a^8*b^4+2*a^8*b^5+14*a^8*b^6+52*a^8*b^7+66*a^8*b^8+17*a^8*b^9+11*a^9+59*a^9*b+50*a^9*b^2+65*a^9*b^3+56*a^9*b^4+45*a^9*b^5+42*a^9*b^6+51*a^9*b^7+47*a^9*b^8+54*a^9*b^9+20*a^9*b^10+a^9*b^11+66*a^9*b^12+17*a^9*b^13+33*a^10*b+a^10*b^2+10*a^10*b^3+56*a^10*b^4+13*a^10*b^6+4*a^10*b^7+66*a^10*b^8+18*a^10*b^9+11*a^11*b^2+3*a^11*b^3+2*a^11*b^5+11*a^11*b^7+a^11*b^10+a^13*b^2+66*a^13*b^3+17*a^13*b^4+a^13*b^5+66*a^13*b^6+18*a^13*b^7+66*a^13*b^8+17*a^13*b^9+a^13*b^10+66*a^13*b^11+17*a^13*b^12+a^14*b+66*a^14*b^2+20*a^14*b^3+a^14*b^4+21*a^14*b^6+66*a^14*b^7+18*a^14*b^8+a^14*b^9+66*a^14*b^10+17*a^14*b^11+11*a^15*b+3*a^15*b^2+14*a^15*b^4+3*a^15*b^5+11*a^15*b^6+2*a^15*b^7+13*a^15*b^9+a^15*b^12+a^16*b^3+a^16*b^8+a^19*b^3+a^19*b^6+a^19*b^8+a^19*b^11+a^20*b^2+a^20*b^5+a^20*b^7+a^20*b^10", domain);

        lMultivariatePolynomialZp[] uFactors = {
                parse("b^2+b^5+b^7+b^10", domain, vars),
                parse("33+a", domain, vars),
                parse("22+a", domain, vars),
                parse("32+8*a+a^2", domain, vars),
                parse("32+59*a+a^2", domain, vars),
                parse("24+5*a+15*a^2+45*a^3+a^4", domain, vars),
                parse("28+56*a+45*a^2+23*a^3+a^4", domain, vars),
                parse("19+a^6", domain, vars)
        };

        lEvaluation evaluation = new lEvaluation(2, new long[]{56}, domain, base.ordering);
        int degree = base.degree(1) + 1;
        for (int i = 0; i < its(10, 100); i++) {
            long start = System.nanoTime();
            lMultivariatePolynomialZp[] lifted = uFactors.clone();
            HenselLifting.bivariateLiftNoLCCorrection0(base, lifted, evaluation, degree);
            System.out.println(TimeUnits.nanosecondsToString(System.nanoTime() - start));
            Assert.assertEquals(base, evaluation.modImage(base.createOne().multiply(lifted), 1, degree));
        }
    }

    @Test
    public void testBivariateLifting2() throws Exception {
        lIntegersModulo domain = new lIntegersModulo(62653);
        String[] vars = {"a", "b"};

        lMultivariatePolynomialZp[] factors = {
                lMultivariatePolynomialZp.parse("17096+6578*a*b^2+54905*a^3", domain, vars),
                lMultivariatePolynomialZp.parse("43370+32368*a^2*b^2+45712*a^2*b^4+52302*a^4+23776*a^4*b^2", domain, vars)
        };

        lMultivariatePolynomialZp base = factors[0].createOne().multiply(factors);
        lEvaluation evaluation = new lEvaluation(2, new long[]{0}, domain, base.ordering);

        lMultivariatePolynomialZp[] uFactors = evaluation.evaluateFrom(factors, 1);

        int degree = base.degree(1) + 1;
        HenselLifting.bivariateLift0(base, uFactors, null, evaluation, degree);
        Assert.assertTrue(evaluation.modImage(base.clone().subtract(base.createOne().multiply(uFactors)), 1, degree).isZero());
    }

    @Test
    public void testBivariateLiftingRandom1() throws Exception {
        MultivariateFactorizationTest.lSampleDecompositionSource baseSource = new MultivariateFactorizationTest.lSampleDecompositionSource(
                3, 5,
                2, 2,
                2, 6,
                1, 4);
        baseSource.minModulusBits = 15;
        baseSource.maxModulusBits = 30;
        FactorizationTestData.SampleDecompositionSource<lMultivariatePolynomialZp> source
                = orderVarsByDegree(
                filterMonomialContent(
                        filterNonSquareFree(baseSource)));
        testHenselLift(source, its(500, 1000), lEvaluationLoop::new,
                ((base, factors, factorsLC, evaluation, degreeBounds, from) ->
                        HenselLifting.bivariateLift0(base, factors, factorsLC, evaluation, degreeBounds[1])), true, 1);
    }

    @Test
    public void testBivariateLiftingRandom2() throws Exception {
        MultivariateFactorizationTest.lSampleDecompositionSource baseSource = new MultivariateFactorizationTest.lSampleDecompositionSource(
                3, 5,
                2, 2,
                2, 6,
                1, 4);
        baseSource.minModulusBits = 15;
        baseSource.maxModulusBits = 30;
        FactorizationTestData.SampleDecompositionSource<lMultivariatePolynomialZp> source
                = orderVarsByDegree(
                filterMonomialContent(
                        filterNonSquareFree(baseSource)));
        testHenselLift(source, its(500, 1000), lEvaluationLoop::new,
                ((base, factors, factorsLC, evaluation, degreeBounds, from) ->
                        HenselLifting.bivariateLiftNoLCCorrection0(base, factors, evaluation, degreeBounds[1])), false, 1);
    }


    @Test
    public void test4() throws Exception {
        PrivateRandom.getRandom().setSeed(50);
        lIntegersModulo domain = new lIntegersModulo(592346501);
        String[] vars = {"a", "b"};
        lMultivariatePolynomialZp
                a = lMultivariatePolynomialZp.parse("8864159 + 332216825*a + 307171438*a^2 + 574396609*a^3 + b", domain, vars),
                b = lMultivariatePolynomialZp.parse("364341910 + 56968290*a + 134477777*a^2 + 264733241*b + 223672725*a*b + 365910146*a^2*b + 448183856*b^2 + 56041492*a*b^2 + 1386*a^2*b^2", domain, vars),
                base = a.clone().multiply(b);

        lEvaluation evaluation = new lEvaluation(base.nVariables, new long[]{0}, domain, base.ordering);

        lMultivariatePolynomialZp[] uFactors = {
                evaluation.evaluateFrom(a, 1),
                evaluation.evaluateFrom(b, 1),
        };

        uFactors[1].multiplyByLC(uFactors[0]);
        uFactors[0].monic();

        System.out.println(uFactors[0]);
        System.out.println(uFactors[1]);

        lMultivariatePolynomialZp[] factorsLC = {
                null,
                base.lc(0)
        };

        HenselLifting.multivariateLift0(base,
                uFactors,
                factorsLC,
                evaluation,
                base.degrees());
        Assert.assertEquals(base, multiply(uFactors));

        System.out.println(a);
        System.out.println(b);
    }

    /* ==================================== Test data =============================================== */

    private interface Lifting<
            Term extends DegreeVector<Term>,
            Poly extends AMultivariatePolynomial<Term, Poly>> {
        void lift(Poly base, Poly[] factors, Poly[] factorsLC, IEvaluation<Term, Poly> evaluation, int[] degreeBounds, int from);
    }

    final class BivariateLift<
            Term extends DegreeVector<Term>,
            Poly extends AMultivariatePolynomial<Term, Poly>>
            implements Lifting<Term, Poly> {
        @Override
        public void lift(Poly base, Poly[] factors, Poly[] factorsLC, IEvaluation<Term, Poly> evaluation, int[] degreeBounds, int from) {
            HenselLifting.bivariateLift0(base, factors, factorsLC, evaluation, degreeBounds[1]);
        }
    }

    public static <Term extends DegreeVector<Term>, Poly extends AMultivariatePolynomial<Term, Poly>>
    void testHenselLift(FactorizationTestData.SampleDecompositionSource<Poly> source, int nIterations,
                        Function<Poly, IEvaluationLoop<Term, Poly>> evalFactory,
                        Lifting<Term, Poly> algorithm, boolean correctLC, int from) {
        System.out.println("Testing Hensel lifting ");
        System.out.println("Input source: " + source);

        DescriptiveStatistics timing = new DescriptiveStatistics();

        int prevProgress = -1, currProgress;
        main:
        for (int n = 0; n < nIterations; n++) {
            if (n == nIterations / 10)
                timing.clear();

            if ((currProgress = (int) (100.0 * n / nIterations)) != prevProgress) {
                prevProgress = currProgress;
                System.out.print(">");
                System.out.flush();
            }
            FactorizationTestData.SampleDecomposition<Poly> sample = source.next();
            if (!allCoprime(sample.factors)) {
                --n;
                continue;
            }
            if (Arrays.stream(sample.factors).anyMatch(p -> p.degree(0) == 0)) {
                --n;
                continue;
            }
            try {

                Poly factory = sample.poly;
                IEvaluationLoop<Term, Poly> evaluations = evalFactory.apply(factory);

                for (int nAttempt = 0; nAttempt < 64; nAttempt++) {
                    IEvaluation<Term, Poly> evaluation = evaluations.next();
                    Poly[] uFactors = Arrays.stream(sample.factors).map(p -> evaluation.evaluateFrom(p, from)).toArray(factory::arrayNewInstance);
                    if (!allCoprime(uFactors))
                        continue;

                    if (!IntStream.range(0, uFactors.length).allMatch(i -> sample.factors[i].degree(0) == uFactors[i].degree(0)))
                        continue;

                    Poly[] factorsLC = Arrays.stream(sample.factors).map(p -> p.lc(0)).toArray(factory::arrayNewInstance);

                    long start = System.nanoTime();
                    algorithm.lift(sample.poly, uFactors, correctLC ? factorsLC : null, evaluation, sample.poly.degrees(), from);
                    timing.addValue(System.nanoTime() - start);


                    if (correctLC || Arrays.stream(factorsLC).allMatch(Poly::isConstant))
                        Assert.assertArrayEquals(sample.factors, uFactors);
                    else
                        Assert.assertEquals(sample.poly, evaluation.modImage(multiply(uFactors), sample.poly.degrees()));
                    continue main;
                }
                throw new RuntimeException();
            } catch (Throwable throwable) {
                System.out.println("\n============ Error ============");
                System.out.println("Domain: " + sample.poly.coefficientDomainToString());
                System.out.println("Polynomial: " + sample.poly);
                System.out.println("Expected factorization: " + Arrays.toString(sample.factors));
                throw throwable;
            }
        }

        System.out.println(source.statisticsToString());

        System.out.println("\n============ Timings ============");
        System.out.println("Stats: " + TimeUnits.statisticsNanotime(timing));
    }

    static <Poly extends IGeneralPolynomial<Poly>> Poly multiply(Poly... p) {
        return p[0].createOne().multiply(p);
    }

    private static <Poly extends AMultivariatePolynomial> boolean allCoprime(Poly... factors) {
        return StreamSupport
                .stream(Spliterators.spliteratorUnknownSize(new IntCombinationsGenerator(factors.length, 2), Spliterator.ORDERED), false)
                .allMatch(arr -> MultivariateGCD.PolynomialGCD(factors[arr[0]], factors[arr[1]]).isOne());
    }
}