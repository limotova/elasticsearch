// Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
// or more contributor license agreements. Licensed under the Elastic License
// 2.0; you may not use this file except in compliance with the Elastic License
// 2.0.
package org.elasticsearch.compute.aggregation;

import java.lang.Integer;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import org.elasticsearch.compute.operator.DriverContext;

/**
 * {@link AggregatorFunctionSupplier} implementation for {@link TopIntAggregator}.
 * This class is generated. Edit {@code AggregatorFunctionSupplierImplementer} instead.
 */
public final class TopIntAggregatorFunctionSupplier implements AggregatorFunctionSupplier {
  private final List<Integer> channels;

  private final int limit;

  private final boolean ascending;

  public TopIntAggregatorFunctionSupplier(List<Integer> channels, int limit, boolean ascending) {
    this.channels = channels;
    this.limit = limit;
    this.ascending = ascending;
  }

  @Override
  public TopIntAggregatorFunction aggregator(DriverContext driverContext) {
    return TopIntAggregatorFunction.create(driverContext, channels, limit, ascending);
  }

  @Override
  public TopIntGroupingAggregatorFunction groupingAggregator(DriverContext driverContext) {
    return TopIntGroupingAggregatorFunction.create(channels, driverContext, limit, ascending);
  }

  @Override
  public String describe() {
    return "top of ints";
  }
}
