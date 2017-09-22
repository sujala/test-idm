package testHelpers;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Ticker;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A Ticker whose value can be advanced programmatically in test.
 * <p>
 * The ticker can be configured so that the time is incremented whenever {@link #read} is called:
 * see {@link #setAutoIncrementStep}.
 * <p>
 * This class is thread-safe.
 *
 * @author Jige Yu
 * @since 10.0
 */
public class FakeTicker extends Ticker {

    private final AtomicLong nanos = new AtomicLong();
    private volatile long autoIncrementStepNanos;

    /** Advances the ticker value by {@code time} in {@code timeUnit}. */
    public FakeTicker advance(long time, TimeUnit timeUnit) {
        return advance(timeUnit.toNanos(time));
    }

    /** Advances the ticker value by {@code nanoseconds}. */
    public FakeTicker advance(long nanoseconds) {
        nanos.addAndGet(nanoseconds);
        return this;
    }

    /**
     * Sets the increment applied to the ticker whenever it is queried.
     *
     * <p>The default behavior is to auto increment by zero. i.e: The ticker is left unchanged when
     * queried.
     */
    public FakeTicker setAutoIncrementStep(long autoIncrementStep, TimeUnit timeUnit) {
        checkArgument(autoIncrementStep >= 0, "May not auto-increment by a negative amount");
        this.autoIncrementStepNanos = timeUnit.toNanos(autoIncrementStep);
        return this;
    }

    @Override public long read() {
        return nanos.getAndAdd(autoIncrementStepNanos);
    }
}
