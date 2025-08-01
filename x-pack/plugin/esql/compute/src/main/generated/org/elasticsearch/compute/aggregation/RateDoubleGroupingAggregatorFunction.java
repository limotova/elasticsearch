// Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
// or more contributor license agreements. Licensed under the Elastic License
// 2.0; you may not use this file except in compliance with the Elastic License
// 2.0.
package org.elasticsearch.compute.aggregation;

import java.lang.Integer;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import java.util.List;
import org.elasticsearch.compute.data.Block;
import org.elasticsearch.compute.data.DoubleBlock;
import org.elasticsearch.compute.data.DoubleVector;
import org.elasticsearch.compute.data.ElementType;
import org.elasticsearch.compute.data.IntArrayBlock;
import org.elasticsearch.compute.data.IntBigArrayBlock;
import org.elasticsearch.compute.data.IntBlock;
import org.elasticsearch.compute.data.IntVector;
import org.elasticsearch.compute.data.LongBlock;
import org.elasticsearch.compute.data.LongVector;
import org.elasticsearch.compute.data.Page;
import org.elasticsearch.compute.operator.DriverContext;

/**
 * {@link GroupingAggregatorFunction} implementation for {@link RateDoubleAggregator}.
 * This class is generated. Edit {@code GroupingAggregatorImplementer} instead.
 */
public final class RateDoubleGroupingAggregatorFunction implements GroupingAggregatorFunction {
  private static final List<IntermediateStateDesc> INTERMEDIATE_STATE_DESC = List.of(
      new IntermediateStateDesc("timestamps", ElementType.LONG),
      new IntermediateStateDesc("values", ElementType.DOUBLE),
      new IntermediateStateDesc("sampleCounts", ElementType.INT),
      new IntermediateStateDesc("resets", ElementType.DOUBLE)  );

  private final RateDoubleAggregator.DoubleRateGroupingState state;

  private final List<Integer> channels;

  private final DriverContext driverContext;

  public RateDoubleGroupingAggregatorFunction(List<Integer> channels,
      RateDoubleAggregator.DoubleRateGroupingState state, DriverContext driverContext) {
    this.channels = channels;
    this.state = state;
    this.driverContext = driverContext;
  }

  public static RateDoubleGroupingAggregatorFunction create(List<Integer> channels,
      DriverContext driverContext) {
    return new RateDoubleGroupingAggregatorFunction(channels, RateDoubleAggregator.initGrouping(driverContext), driverContext);
  }

  public static List<IntermediateStateDesc> intermediateStateDesc() {
    return INTERMEDIATE_STATE_DESC;
  }

  @Override
  public int intermediateBlockCount() {
    return INTERMEDIATE_STATE_DESC.size();
  }

  @Override
  public GroupingAggregatorFunction.AddInput prepareProcessRawInputPage(SeenGroupIds seenGroupIds,
      Page page) {
    DoubleBlock valuesBlock = page.getBlock(channels.get(0));
    DoubleVector valuesVector = valuesBlock.asVector();
    LongBlock timestampsBlock = page.getBlock(channels.get(1));
    LongVector timestampsVector = timestampsBlock.asVector();
    if (timestampsVector == null)  {
      throw new IllegalStateException("expected @timestamp vector; but got a block");
    }
    if (valuesVector == null) {
      if (valuesBlock.mayHaveNulls()) {
        state.enableGroupIdTracking(seenGroupIds);
      }
      return new GroupingAggregatorFunction.AddInput() {
        @Override
        public void add(int positionOffset, IntArrayBlock groupIds) {
          addRawInput(positionOffset, groupIds, valuesBlock, timestampsVector);
        }

        @Override
        public void add(int positionOffset, IntBigArrayBlock groupIds) {
          addRawInput(positionOffset, groupIds, valuesBlock, timestampsVector);
        }

        @Override
        public void add(int positionOffset, IntVector groupIds) {
          addRawInput(positionOffset, groupIds, valuesBlock, timestampsVector);
        }

        @Override
        public void close() {
        }
      };
    }
    return new GroupingAggregatorFunction.AddInput() {
      @Override
      public void add(int positionOffset, IntArrayBlock groupIds) {
        addRawInput(positionOffset, groupIds, valuesVector, timestampsVector);
      }

      @Override
      public void add(int positionOffset, IntBigArrayBlock groupIds) {
        addRawInput(positionOffset, groupIds, valuesVector, timestampsVector);
      }

      @Override
      public void add(int positionOffset, IntVector groupIds) {
        addRawInput(positionOffset, groupIds, valuesVector, timestampsVector);
      }

      @Override
      public void close() {
      }
    };
  }

  private void addRawInput(int positionOffset, IntArrayBlock groups, DoubleBlock values,
      LongVector timestamps) {
    for (int groupPosition = 0; groupPosition < groups.getPositionCount(); groupPosition++) {
      if (groups.isNull(groupPosition) || values.isNull(groupPosition + positionOffset)) {
        continue;
      }
      int groupStart = groups.getFirstValueIndex(groupPosition);
      int groupEnd = groupStart + groups.getValueCount(groupPosition);
      for (int g = groupStart; g < groupEnd; g++) {
        int groupId = groups.getInt(g);
        int valuesStart = values.getFirstValueIndex(groupPosition + positionOffset);
        int valuesEnd = valuesStart + values.getValueCount(groupPosition + positionOffset);
        for (int v = valuesStart; v < valuesEnd; v++) {
          RateDoubleAggregator.combine(state, groupId, timestamps.getLong(v), values.getDouble(v));
        }
      }
    }
  }

  private void addRawInput(int positionOffset, IntArrayBlock groups, DoubleVector values,
      LongVector timestamps) {
    for (int groupPosition = 0; groupPosition < groups.getPositionCount(); groupPosition++) {
      if (groups.isNull(groupPosition)) {
        continue;
      }
      int groupStart = groups.getFirstValueIndex(groupPosition);
      int groupEnd = groupStart + groups.getValueCount(groupPosition);
      for (int g = groupStart; g < groupEnd; g++) {
        int groupId = groups.getInt(g);
        var valuePosition = groupPosition + positionOffset;
        RateDoubleAggregator.combine(state, groupId, timestamps.getLong(valuePosition), values.getDouble(valuePosition));
      }
    }
  }

  @Override
  public void addIntermediateInput(int positionOffset, IntArrayBlock groups, Page page) {
    state.enableGroupIdTracking(new SeenGroupIds.Empty());
    assert channels.size() == intermediateBlockCount();
    Block timestampsUncast = page.getBlock(channels.get(0));
    if (timestampsUncast.areAllValuesNull()) {
      return;
    }
    LongBlock timestamps = (LongBlock) timestampsUncast;
    Block valuesUncast = page.getBlock(channels.get(1));
    if (valuesUncast.areAllValuesNull()) {
      return;
    }
    DoubleBlock values = (DoubleBlock) valuesUncast;
    Block sampleCountsUncast = page.getBlock(channels.get(2));
    if (sampleCountsUncast.areAllValuesNull()) {
      return;
    }
    IntVector sampleCounts = ((IntBlock) sampleCountsUncast).asVector();
    Block resetsUncast = page.getBlock(channels.get(3));
    if (resetsUncast.areAllValuesNull()) {
      return;
    }
    DoubleVector resets = ((DoubleBlock) resetsUncast).asVector();
    assert timestamps.getPositionCount() == values.getPositionCount() && timestamps.getPositionCount() == sampleCounts.getPositionCount() && timestamps.getPositionCount() == resets.getPositionCount();
    for (int groupPosition = 0; groupPosition < groups.getPositionCount(); groupPosition++) {
      if (groups.isNull(groupPosition)) {
        continue;
      }
      int groupStart = groups.getFirstValueIndex(groupPosition);
      int groupEnd = groupStart + groups.getValueCount(groupPosition);
      for (int g = groupStart; g < groupEnd; g++) {
        int groupId = groups.getInt(g);
        RateDoubleAggregator.combineIntermediate(state, groupId, timestamps, values, sampleCounts.getInt(groupPosition + positionOffset), resets.getDouble(groupPosition + positionOffset), groupPosition + positionOffset);
      }
    }
  }

  private void addRawInput(int positionOffset, IntBigArrayBlock groups, DoubleBlock values,
      LongVector timestamps) {
    for (int groupPosition = 0; groupPosition < groups.getPositionCount(); groupPosition++) {
      if (groups.isNull(groupPosition) || values.isNull(groupPosition + positionOffset)) {
        continue;
      }
      int groupStart = groups.getFirstValueIndex(groupPosition);
      int groupEnd = groupStart + groups.getValueCount(groupPosition);
      for (int g = groupStart; g < groupEnd; g++) {
        int groupId = groups.getInt(g);
        int valuesStart = values.getFirstValueIndex(groupPosition + positionOffset);
        int valuesEnd = valuesStart + values.getValueCount(groupPosition + positionOffset);
        for (int v = valuesStart; v < valuesEnd; v++) {
          RateDoubleAggregator.combine(state, groupId, timestamps.getLong(v), values.getDouble(v));
        }
      }
    }
  }

  private void addRawInput(int positionOffset, IntBigArrayBlock groups, DoubleVector values,
      LongVector timestamps) {
    for (int groupPosition = 0; groupPosition < groups.getPositionCount(); groupPosition++) {
      if (groups.isNull(groupPosition)) {
        continue;
      }
      int groupStart = groups.getFirstValueIndex(groupPosition);
      int groupEnd = groupStart + groups.getValueCount(groupPosition);
      for (int g = groupStart; g < groupEnd; g++) {
        int groupId = groups.getInt(g);
        var valuePosition = groupPosition + positionOffset;
        RateDoubleAggregator.combine(state, groupId, timestamps.getLong(valuePosition), values.getDouble(valuePosition));
      }
    }
  }

  @Override
  public void addIntermediateInput(int positionOffset, IntBigArrayBlock groups, Page page) {
    state.enableGroupIdTracking(new SeenGroupIds.Empty());
    assert channels.size() == intermediateBlockCount();
    Block timestampsUncast = page.getBlock(channels.get(0));
    if (timestampsUncast.areAllValuesNull()) {
      return;
    }
    LongBlock timestamps = (LongBlock) timestampsUncast;
    Block valuesUncast = page.getBlock(channels.get(1));
    if (valuesUncast.areAllValuesNull()) {
      return;
    }
    DoubleBlock values = (DoubleBlock) valuesUncast;
    Block sampleCountsUncast = page.getBlock(channels.get(2));
    if (sampleCountsUncast.areAllValuesNull()) {
      return;
    }
    IntVector sampleCounts = ((IntBlock) sampleCountsUncast).asVector();
    Block resetsUncast = page.getBlock(channels.get(3));
    if (resetsUncast.areAllValuesNull()) {
      return;
    }
    DoubleVector resets = ((DoubleBlock) resetsUncast).asVector();
    assert timestamps.getPositionCount() == values.getPositionCount() && timestamps.getPositionCount() == sampleCounts.getPositionCount() && timestamps.getPositionCount() == resets.getPositionCount();
    for (int groupPosition = 0; groupPosition < groups.getPositionCount(); groupPosition++) {
      if (groups.isNull(groupPosition)) {
        continue;
      }
      int groupStart = groups.getFirstValueIndex(groupPosition);
      int groupEnd = groupStart + groups.getValueCount(groupPosition);
      for (int g = groupStart; g < groupEnd; g++) {
        int groupId = groups.getInt(g);
        RateDoubleAggregator.combineIntermediate(state, groupId, timestamps, values, sampleCounts.getInt(groupPosition + positionOffset), resets.getDouble(groupPosition + positionOffset), groupPosition + positionOffset);
      }
    }
  }

  private void addRawInput(int positionOffset, IntVector groups, DoubleBlock values,
      LongVector timestamps) {
    for (int groupPosition = 0; groupPosition < groups.getPositionCount(); groupPosition++) {
      if (values.isNull(groupPosition + positionOffset)) {
        continue;
      }
      int groupId = groups.getInt(groupPosition);
      int valuesStart = values.getFirstValueIndex(groupPosition + positionOffset);
      int valuesEnd = valuesStart + values.getValueCount(groupPosition + positionOffset);
      for (int v = valuesStart; v < valuesEnd; v++) {
        RateDoubleAggregator.combine(state, groupId, timestamps.getLong(v), values.getDouble(v));
      }
    }
  }

  private void addRawInput(int positionOffset, IntVector groups, DoubleVector values,
      LongVector timestamps) {
    for (int groupPosition = 0; groupPosition < groups.getPositionCount(); groupPosition++) {
      int groupId = groups.getInt(groupPosition);
      var valuePosition = groupPosition + positionOffset;
      RateDoubleAggregator.combine(state, groupId, timestamps.getLong(valuePosition), values.getDouble(valuePosition));
    }
  }

  @Override
  public void addIntermediateInput(int positionOffset, IntVector groups, Page page) {
    state.enableGroupIdTracking(new SeenGroupIds.Empty());
    assert channels.size() == intermediateBlockCount();
    Block timestampsUncast = page.getBlock(channels.get(0));
    if (timestampsUncast.areAllValuesNull()) {
      return;
    }
    LongBlock timestamps = (LongBlock) timestampsUncast;
    Block valuesUncast = page.getBlock(channels.get(1));
    if (valuesUncast.areAllValuesNull()) {
      return;
    }
    DoubleBlock values = (DoubleBlock) valuesUncast;
    Block sampleCountsUncast = page.getBlock(channels.get(2));
    if (sampleCountsUncast.areAllValuesNull()) {
      return;
    }
    IntVector sampleCounts = ((IntBlock) sampleCountsUncast).asVector();
    Block resetsUncast = page.getBlock(channels.get(3));
    if (resetsUncast.areAllValuesNull()) {
      return;
    }
    DoubleVector resets = ((DoubleBlock) resetsUncast).asVector();
    assert timestamps.getPositionCount() == values.getPositionCount() && timestamps.getPositionCount() == sampleCounts.getPositionCount() && timestamps.getPositionCount() == resets.getPositionCount();
    for (int groupPosition = 0; groupPosition < groups.getPositionCount(); groupPosition++) {
      int groupId = groups.getInt(groupPosition);
      RateDoubleAggregator.combineIntermediate(state, groupId, timestamps, values, sampleCounts.getInt(groupPosition + positionOffset), resets.getDouble(groupPosition + positionOffset), groupPosition + positionOffset);
    }
  }

  @Override
  public void selectedMayContainUnseenGroups(SeenGroupIds seenGroupIds) {
    state.enableGroupIdTracking(seenGroupIds);
  }

  @Override
  public void evaluateIntermediate(Block[] blocks, int offset, IntVector selected) {
    state.toIntermediate(blocks, offset, selected, driverContext);
  }

  @Override
  public void evaluateFinal(Block[] blocks, int offset, IntVector selected,
      GroupingAggregatorEvaluationContext evaluatorContext) {
    blocks[offset] = RateDoubleAggregator.evaluateFinal(state, selected, evaluatorContext);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName()).append("[");
    sb.append("channels=").append(channels);
    sb.append("]");
    return sb.toString();
  }

  @Override
  public void close() {
    state.close();
  }
}
