// Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
// or more contributor license agreements. Licensed under the Elastic License
// 2.0; you may not use this file except in compliance with the Elastic License
// 2.0.
package org.elasticsearch.xpack.esql.expression.function.scalar.math;

import java.lang.ArithmeticException;
import java.lang.IllegalArgumentException;
import java.lang.Override;
import java.lang.String;
import org.elasticsearch.compute.data.Block;
import org.elasticsearch.compute.data.LongBlock;
import org.elasticsearch.compute.data.LongVector;
import org.elasticsearch.compute.data.Page;
import org.elasticsearch.compute.operator.DriverContext;
import org.elasticsearch.compute.operator.EvalOperator;
import org.elasticsearch.compute.operator.Warnings;
import org.elasticsearch.core.Releasables;
import org.elasticsearch.xpack.esql.core.tree.Source;

/**
 * {@link EvalOperator.ExpressionEvaluator} implementation for {@link Round}.
 * This class is generated. Edit {@code EvaluatorImplementer} instead.
 */
public final class RoundUnsignedLongEvaluator implements EvalOperator.ExpressionEvaluator {
  private final Source source;

  private final EvalOperator.ExpressionEvaluator val;

  private final EvalOperator.ExpressionEvaluator decimals;

  private final DriverContext driverContext;

  private Warnings warnings;

  public RoundUnsignedLongEvaluator(Source source, EvalOperator.ExpressionEvaluator val,
      EvalOperator.ExpressionEvaluator decimals, DriverContext driverContext) {
    this.source = source;
    this.val = val;
    this.decimals = decimals;
    this.driverContext = driverContext;
  }

  @Override
  public Block eval(Page page) {
    try (LongBlock valBlock = (LongBlock) val.eval(page)) {
      try (LongBlock decimalsBlock = (LongBlock) decimals.eval(page)) {
        LongVector valVector = valBlock.asVector();
        if (valVector == null) {
          return eval(page.getPositionCount(), valBlock, decimalsBlock);
        }
        LongVector decimalsVector = decimalsBlock.asVector();
        if (decimalsVector == null) {
          return eval(page.getPositionCount(), valBlock, decimalsBlock);
        }
        return eval(page.getPositionCount(), valVector, decimalsVector);
      }
    }
  }

  public LongBlock eval(int positionCount, LongBlock valBlock, LongBlock decimalsBlock) {
    try(LongBlock.Builder result = driverContext.blockFactory().newLongBlockBuilder(positionCount)) {
      position: for (int p = 0; p < positionCount; p++) {
        if (valBlock.isNull(p)) {
          result.appendNull();
          continue position;
        }
        if (valBlock.getValueCount(p) != 1) {
          if (valBlock.getValueCount(p) > 1) {
            warnings().registerException(new IllegalArgumentException("single-value function encountered multi-value"));
          }
          result.appendNull();
          continue position;
        }
        if (decimalsBlock.isNull(p)) {
          result.appendNull();
          continue position;
        }
        if (decimalsBlock.getValueCount(p) != 1) {
          if (decimalsBlock.getValueCount(p) > 1) {
            warnings().registerException(new IllegalArgumentException("single-value function encountered multi-value"));
          }
          result.appendNull();
          continue position;
        }
        try {
          result.appendLong(Round.processUnsignedLong(valBlock.getLong(valBlock.getFirstValueIndex(p)), decimalsBlock.getLong(decimalsBlock.getFirstValueIndex(p))));
        } catch (ArithmeticException e) {
          warnings().registerException(e);
          result.appendNull();
        }
      }
      return result.build();
    }
  }

  public LongBlock eval(int positionCount, LongVector valVector, LongVector decimalsVector) {
    try(LongBlock.Builder result = driverContext.blockFactory().newLongBlockBuilder(positionCount)) {
      position: for (int p = 0; p < positionCount; p++) {
        try {
          result.appendLong(Round.processUnsignedLong(valVector.getLong(p), decimalsVector.getLong(p)));
        } catch (ArithmeticException e) {
          warnings().registerException(e);
          result.appendNull();
        }
      }
      return result.build();
    }
  }

  @Override
  public String toString() {
    return "RoundUnsignedLongEvaluator[" + "val=" + val + ", decimals=" + decimals + "]";
  }

  @Override
  public void close() {
    Releasables.closeExpectNoException(val, decimals);
  }

  private Warnings warnings() {
    if (warnings == null) {
      this.warnings = Warnings.createWarnings(
              driverContext.warningsMode(),
              source.source().getLineNumber(),
              source.source().getColumnNumber(),
              source.text()
          );
    }
    return warnings;
  }

  static class Factory implements EvalOperator.ExpressionEvaluator.Factory {
    private final Source source;

    private final EvalOperator.ExpressionEvaluator.Factory val;

    private final EvalOperator.ExpressionEvaluator.Factory decimals;

    public Factory(Source source, EvalOperator.ExpressionEvaluator.Factory val,
        EvalOperator.ExpressionEvaluator.Factory decimals) {
      this.source = source;
      this.val = val;
      this.decimals = decimals;
    }

    @Override
    public RoundUnsignedLongEvaluator get(DriverContext context) {
      return new RoundUnsignedLongEvaluator(source, val.get(context), decimals.get(context), context);
    }

    @Override
    public String toString() {
      return "RoundUnsignedLongEvaluator[" + "val=" + val + ", decimals=" + decimals + "]";
    }
  }
}
