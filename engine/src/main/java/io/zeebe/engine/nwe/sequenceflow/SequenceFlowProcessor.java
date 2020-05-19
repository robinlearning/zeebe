package io.zeebe.engine.nwe.sequenceflow;

import io.zeebe.engine.nwe.BpmnElementContext;
import io.zeebe.engine.nwe.BpmnElementProcessor;
import io.zeebe.engine.nwe.behavior.BpmnBehaviors;
import io.zeebe.engine.nwe.behavior.BpmnDeferredRecordsBehavior;
import io.zeebe.engine.nwe.behavior.BpmnStateBehavior;
import io.zeebe.engine.nwe.behavior.BpmnStateTransitionBehavior;
import io.zeebe.engine.processor.workflow.deployment.model.element.ExecutableFlowNode;
import io.zeebe.engine.processor.workflow.deployment.model.element.ExecutableSequenceFlow;
import io.zeebe.protocol.record.intent.WorkflowInstanceIntent;
import io.zeebe.protocol.record.value.BpmnElementType;
import java.util.stream.Collectors;

/**
 * A sequence flow doesn't have a proper lifecycle as the other BPMN elements. It can only be taken.
 * However, it implements the same interface to keep the rest of implementation simple. But only the
 * method {@link #onActivating(ExecutableSequenceFlow, BpmnElementContext)} perform the action.
 * Calling other methods causes an exception.
 */
public final class SequenceFlowProcessor implements BpmnElementProcessor<ExecutableSequenceFlow> {

  private final BpmnStateTransitionBehavior stateTransitionBehavior;
  private final BpmnStateBehavior stateBehavior;
  private final BpmnDeferredRecordsBehavior deferredRecordsBehavior;

  public SequenceFlowProcessor(final BpmnBehaviors bpmnBehaviors) {
    stateTransitionBehavior = bpmnBehaviors.stateTransitionBehavior();
    stateBehavior = bpmnBehaviors.stateBehavior();
    deferredRecordsBehavior = bpmnBehaviors.deferredRecordsBehavior();
  }

  @Override
  public Class<ExecutableSequenceFlow> getType() {
    return ExecutableSequenceFlow.class;
  }

  @Override
  public void onActivating(final ExecutableSequenceFlow element, final BpmnElementContext context) {
    onSequenceFlowTaken(element, context);
  }

  @Override
  public void onActivated(final ExecutableSequenceFlow element, final BpmnElementContext context) {
    throw new UnsupportedSequenceFlowOperationException(context);
  }

  @Override
  public void onCompleting(final ExecutableSequenceFlow element, final BpmnElementContext context) {
    throw new UnsupportedSequenceFlowOperationException(context);
  }

  @Override
  public void onCompleted(final ExecutableSequenceFlow element, final BpmnElementContext context) {
    throw new UnsupportedSequenceFlowOperationException(context);
  }

  @Override
  public void onTerminating(
      final ExecutableSequenceFlow element, final BpmnElementContext context) {
    throw new UnsupportedSequenceFlowOperationException(context);
  }

  @Override
  public void onTerminated(final ExecutableSequenceFlow element, final BpmnElementContext context) {
    throw new UnsupportedSequenceFlowOperationException(context);
  }

  @Override
  public void onEventOccurred(
      final ExecutableSequenceFlow element, final BpmnElementContext context) {
    throw new UnsupportedSequenceFlowOperationException(context);
  }

  private void onSequenceFlowTaken(
      final ExecutableSequenceFlow element, final BpmnElementContext context) {

    final var targetElement = element.getTarget();

    if (targetElement.getElementType() == BpmnElementType.PARALLEL_GATEWAY) {
      joinOnParallelGateway(targetElement, context);

    } else {
      stateTransitionBehavior.activateElementInstanceInFlowScope(context, targetElement);
    }
  }

  private void joinOnParallelGateway(
      final ExecutableFlowNode parallelGateway, final BpmnElementContext context) {

    // before the parallel gateway is activated, each incoming sequence flow of the gateway must be
    // taken (at least once)

    // if a sequence flow is taken more than once then this redundant token remains for the next
    // activation of the gateway (Tetris principle)

    final var flowScopeContext = stateBehavior.getFlowScopeContext(context);
    final var incomingSequenceFlows = parallelGateway.getIncoming();

    // store which sequence flows are taken as deferred records
    deferredRecordsBehavior.deferNewRecord(
        flowScopeContext,
        context.getElementInstanceKey(),
        context.getRecordValue(),
        context.getIntent());

    // TODO (saig0): calculate the ids in the model before
    final var sequenceFlowIds =
        incomingSequenceFlows.stream()
            .map(ExecutableSequenceFlow::getId)
            .collect(Collectors.toList());

    final var tokensBySequenceFlow =
        deferredRecordsBehavior.getDeferredRecords(flowScopeContext).stream()
            .filter(record -> record.getState() == WorkflowInstanceIntent.SEQUENCE_FLOW_TAKEN)
            .filter(record -> sequenceFlowIds.contains(record.getValue().getElementIdBuffer()))
            .collect(Collectors.groupingBy(record -> record.getValue().getElementIdBuffer()));

    if (tokensBySequenceFlow.size() == incomingSequenceFlows.size()) {
      // all incoming sequence flows are taken, so the gateway can be activated

      final var flowScopeInstance = stateBehavior.getFlowScopeInstance(context);

      // consume one token per sequence flow
      tokensBySequenceFlow.forEach(
          (sequenceFlow, tokens) -> {
            final var firstToken = tokens.get(0);
            deferredRecordsBehavior.removeDeferredRecord(flowScopeContext, firstToken);

            flowScopeInstance.consumeToken();
          });

      // spawn a new token for the activated gateway
      flowScopeInstance.spawnToken();
      stateBehavior.updateElementInstance(flowScopeInstance);

      stateTransitionBehavior.activateElementInstanceInFlowScope(context, parallelGateway);
    }
  }

  private static final class UnsupportedSequenceFlowOperationException
      extends UnsupportedOperationException {

    private static final String MESSAGE =
        "This is not the method you're looking for. [context: %s]";

    private UnsupportedSequenceFlowOperationException(final BpmnElementContext context) {
      super(String.format(MESSAGE, context));
    }
  }
}
