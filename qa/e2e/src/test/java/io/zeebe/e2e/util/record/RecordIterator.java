package io.zeebe.e2e.util.record;

import static org.awaitility.Awaitility.await;

import io.zeebe.protocol.record.Record;
import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

@SuppressWarnings("java:S899")
public final class RecordIterator implements Iterator<Record<?>> {
  private final List<Record<?>> records;
  private final Lock lock;
  private final Condition isEmpty;
  private final Duration timeout;
  private int nextIndex = 0;

  public RecordIterator(
      final List<Record<?>> records,
      final Lock lock,
      final Condition isEmpty,
      final Duration timeout) {
    this.records = records;
    this.lock = lock;
    this.isEmpty = isEmpty;
    this.timeout = timeout;
  }

  private boolean isEmpty() {
    return nextIndex >= records.size();
  }

  @Override
  public boolean hasNext() {
    try {
      lock.lockInterruptibly();
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
    }

    try {
      var remainingTime = timeout.toMillis();
      var awaitStart = System.currentTimeMillis();

      while (isEmpty() && remainingTime > 0) {
        try {
          isEmpty.await(remainingTime, TimeUnit.MILLISECONDS);
          remainingTime = System.currentTimeMillis() - awaitStart;
        } catch (final InterruptedException e) {
          Thread.currentThread().interrupt();
          break;
        }
      }

      return !isEmpty();
    } finally {
      lock.unlock();
    }
  }

  @Override
  public Record<?> next() {
    final var index = nextIndex++;
    if (index >= records.size()) {
      throw new NoSuchElementException();
    }

    return records.get(index);
  }
}
