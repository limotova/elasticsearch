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
 * {@link AggregatorFunctionSupplier} implementation for {@link PercentileLongAggregator}.
 * This class is generated. Edit {@code AggregatorFunctionSupplierImplementer} instead.
 */
public final class PercentileLongAggregatorFunctionSupplier implements AggregatorFunctionSupplier {
  private final List<Integer> channels;

  private final double percentile;

  public PercentileLongAggregatorFunctionSupplier(List<Integer> channels, double percentile) {
    this.channels = channels;
    this.percentile = percentile;
  }

  @Override
  public PercentileLongAggregatorFunction aggregator(DriverContext driverContext) {
    return PercentileLongAggregatorFunction.create(driverContext, channels, percentile);
  }

  @Override
  public PercentileLongGroupingAggregatorFunction groupingAggregator(DriverContext driverContext) {
    return PercentileLongGroupingAggregatorFunction.create(channels, driverContext, percentile);
  }

  @Override
  public String describe() {
    return "percentile of longs";
  }
}
