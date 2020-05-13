package io.zeebe.dispatcher.impl.log;

@FunctionalInterface
public interface QuadConsumer<A, B, C, D> {
  // TODO: remove the fourth parameter, rename to TriConsumer and remove the TriConsumer in
  // UpgradeTestCase
  void test(A entry, B position, C fragmentOffset, D todoRemove);
}
