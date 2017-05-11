package cc.r2.core.poly;

import cc.r2.core.number.BigInteger;
import org.apache.commons.math3.random.RandomGenerator;

import java.lang.reflect.Array;
import java.util.Comparator;

/**
 * Domain specification.
 *
 * @author Stanislav Poslavsky
 * @since 1.0
 */
public interface Domain<E> extends Comparator<E> {
    /**
     * Returns whether this domain is a field
     *
     * @return whether this domain is a field
     */
    boolean isField();

    /**
     * Returns number of elements in this domain (cardinality) or null if domain is infinite
     *
     * @return number of elements in this domain (cardinality) or null if domain is infinite
     */
    BigInteger size();

    /**
     * Returns whether this domain is finite
     *
     * @return whether this domain is finite
     */
    default boolean isFinite() {
        return size() != null;
    }

    /**
     * Returns whether this domain is a finite finite
     *
     * @return whether this domain is a finite finite
     */
    default boolean isFiniteField() {
        return isField() && isFinite();
    }


    /**
     * Adds two elements
     *
     * @param a the first element
     * @param b the second element
     * @return a + b
     */
    E add(E a, E b);

    /**
     * Subtracts {@code b} from {@code a}
     *
     * @param a the first element
     * @param b the second element
     * @return a - b
     */
    E subtract(E a, E b);

    /**
     * Multiplies two elements
     *
     * @param a the first element
     * @param b the second element
     * @return a * b
     */
    E multiply(E a, E b);

    /**
     * Negates a given element
     *
     * @param val the domain element
     * @return -val
     */
    E negate(E val);

    /**
     * Returns -1 if {@code a < 0}, 0 if {@code a == 0} and 1 if {@code a > 0}, where comparison is defined in terms
     * of {@link #compare(Object, Object)}
     *
     * @param a the element
     * @return -1 if {@code a < 0}, 0 if {@code a == 0} and 1 otherwise
     */
    int signum(E a);

    /**
     * Returns quotient and remainder of {@code a/b}
     *
     * @param a the dividend
     * @param b the divider
     * @return {@code {quotient, remainder}}
     */
    E[] divideAndRemainder(E a, E b);

    default E divideExact(E a, E b) {
        E[] qd = divideAndRemainder(a, b);
        if (!isZero(qd[1]))
            throw new ArithmeticException("not divisible: " + a + " / " + b);
        return qd[0];
    }

    default E divideOrNull(E a, E b) {
        if (isOne(b))
            return a;
        E[] qd = divideAndRemainder(a, b);
        if (!isZero(qd[1]))
            return null;
        return qd[0];
    }

    /**
     * Gives the inverse element a^-1
     *
     * @param a the element
     * @return a^-1
     */
    E reciprocal(E a);

    /**
     * Returns greatest common divisor of two elements
     *
     * @param a the first element
     * @param b the second element
     * @return gcd
     */
    E gcd(E a, E b);

    /**
     * Domain element representing zero
     *
     * @return 0
     */
    E getZero();

    /**
     * Domain element representing one
     *
     * @return 1
     */
    E getOne();

    /**
     * Domain element representing minus one
     *
     * @return -1
     */
    default E getNegativeOne() {
        return negate(getOne());
    }

    /**
     * Tests whether specified element is zero
     *
     * @param e the domain element
     * @return whether specified element is zero
     */
    boolean isZero(E e);

    /**
     * Tests whether specified element is one
     *
     * @param e the domain element
     * @return whether specified element is one
     */
    boolean isOne(E e);

    /**
     * Tests whether specified element is minus one
     *
     * @param e the domain element
     * @return whether specified element is minus one
     */
    default boolean isMinusOne(E e) {
        return negate(getOne()).equals(e);
    }

    /**
     * Value of machine integer (if supported)
     *
     * @param val machine integer
     * @return domain element associated with specified {@code long}
     */
    E valueOf(long val);

    default E[] valueOf(long[] vals) {
        E[] array = createArray(vals.length);
        for (int i = 0; i < vals.length; i++)
            array[i] = valueOf(vals[i]);
        return array;
    }

    /**
     * Converts a value from possibly other domain to this domain.
     *
     * @param val some element from any domain
     * @return this domain element associated with specified {@code val}
     */
    E valueOf(E val);

    default void setToValueOf(E[] data) {
        for (int i = 0; i < data.length; i++)
            data[i] = valueOf(data[i]);
    }

    /**
     * Parse string into domain element
     *
     * @param string string
     * @return domain element
     */
    default E parse(String string) {
        throw new UnsupportedOperationException();
    }

    /**
     * Creates generic array of domain elements of specified length
     *
     * @param length array length
     * @return array of domain elements of specified {@code length}
     */
    @SuppressWarnings("unchecked")
    default E[] createArray(int length) {
        return (E[]) Array.newInstance(getOne().getClass(), length);
    }

    /**
     * Creates generic array of domain elements of specified length
     *
     * @param length array length
     * @return array of domain elements of specified {@code length}
     */
    @SuppressWarnings("unchecked")
    default E[] createZeroesArray(int length) {
        E[] array = createArray(length);
        for (int i = 0; i < array.length; i++)
            // NOTE: getZero() is invoked each time in a loop in order to fill array with unique elements
            array[i] = getZero();
        return array;
    }

    /**
     * Creates generic array of {@code {a, b}}
     *
     * @param a the first element of array
     * @param b the second element of array
     * @return array {@code {a,b}}
     */
    @SuppressWarnings("unchecked")
    default E[] createArray(E a, E b) {
        E[] array = createArray(2);
        array[0] = a;
        array[1] = b;
        return array;
    }

    /**
     * Returns {@code base} in a power of {@code exponent} (non negative)
     *
     * @param base     base
     * @param exponent exponent (non negative)
     * @return {@code base} in a power of {@code exponent}
     * @throws ArithmeticException if the result overflows a long
     */
    default E pow(E base, int exponent) {
        if (exponent < 0)
            throw new IllegalArgumentException();

        E result = getOne();
        E k2p = base;
        for (; ; ) {
            if ((exponent&1) != 0)
                result = multiply(result, k2p);
            exponent = exponent >> 1;
            if (exponent == 0)
                return result;
            k2p = multiply(k2p, k2p);
        }
    }

//    /**
//     * Returns the element which is next to the specified {@code element} (according to {@link #compare(Object, Object)})
//     * or {@code null} in the case of infinite cardinality
//     *
//     * @param element the element
//     * @return next element
//     */
//    E nextElement(E element);

    /**
     * Returns a random element from this domain
     *
     * @param rnd the source of randomness
     * @return random element from this domain
     */
    default E randomElement(RandomGenerator rnd) { return valueOf(rnd.nextLong());}

    /**
     * Returns domain with larger cardinality that contains all elements of this or null if there is no such domain.
     *
     * @return domain with larger cardinality that contains all elements of this or null if there is no such domain
     */
    Domain<E> getExtension();
}