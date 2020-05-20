/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.0. You may not use this file
 * except in compliance with the Zeebe Community License 1.0.
 */
package io.zeebe.engine.nwe.behavior;

import io.zeebe.engine.nwe.BpmnElementContext;
import io.zeebe.engine.state.ZeebeState;
import io.zeebe.engine.state.instance.ElementInstanceState;
import io.zeebe.engine.state.instance.IndexedRecord;
import io.zeebe.engine.state.instance.StoredRecord.Purpose;
import io.zeebe.protocol.impl.record.value.workflowinstance.WorkflowInstanceRecord;
import io.zeebe.protocol.record.intent.WorkflowInstanceIntent;
import java.util.List;

public final class BpmnDeferredRecordsBehavior {

  private final ElementInstanceState elementInstanceState;

  public BpmnDeferredRecordsBehavior(final ZeebeState zeebeState) {
    elementInstanceState = zeebeState.getWorkflowState().getElementInstanceState();
  }

  public void deferNewRecord(
      final BpmnElementContext context,
      final long key,
      final WorkflowInstanceRecord value,
      final WorkflowInstanceIntent state) {

    elementInstanceState.storeRecord(
        key, context.getElementInstanceKey(), value, state, Purpose.DEFERRED);
  }

  public List<IndexedRecord> getDeferredRecords(final BpmnElementContext context) {
    return elementInstanceState.getDeferredRecords(context.getElementInstanceKey());
  }

  public void removeDeferredRecord(
      final BpmnElementContext context, final IndexedRecord deferredRecord) {
    elementInstanceState.removeStoredRecord(
        context.getElementInstanceKey(), deferredRecord.getKey(), Purpose.DEFERRED);
  }
}
