/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the "Elastic License
 * 2.0", the "GNU Affero General Public License v3.0 only", and the "Server Side
 * Public License v 1"; you may not use this file except in compliance with, at
 * your election, the "Elastic License 2.0", the "GNU Affero General Public
 * License v3.0 only", or the "Server Side Public License, v 1".
 */

package org.elasticsearch.index.mapper;

import org.apache.lucene.sandbox.document.HalfFloatPoint;
import org.elasticsearch.index.mapper.NumberFieldMapper.NumberType;
import org.elasticsearch.xcontent.XContentBuilder;
import org.junit.AssumptionViolatedException;

import java.io.IOException;
import java.util.List;

public class HalfFloatFieldMapperTests extends NumberFieldMapperTests {

    @Override
    protected Number missingValue() {
        return 123f;
    }

    @Override
    protected List<NumberTypeOutOfRangeSpec> outOfRangeSpecs() {
        return List.of(
            NumberTypeOutOfRangeSpec.of(NumberType.HALF_FLOAT, "65520", "[half_float] supports only finite values"),
            NumberTypeOutOfRangeSpec.of(NumberType.HALF_FLOAT, "-65520", "[half_float] supports only finite values"),
            NumberTypeOutOfRangeSpec.of(NumberType.HALF_FLOAT, "-65520", "[half_float] supports only finite values"),
            NumberTypeOutOfRangeSpec.of(NumberType.HALF_FLOAT, Float.NaN, "[half_float] supports only finite values"),
            NumberTypeOutOfRangeSpec.of(NumberType.HALF_FLOAT, Float.POSITIVE_INFINITY, "[half_float] supports only finite values"),
            NumberTypeOutOfRangeSpec.of(NumberType.HALF_FLOAT, Float.NEGATIVE_INFINITY, "[half_float] supports only finite values")
        );
    }

    @Override
    protected void minimalMapping(XContentBuilder b) throws IOException {
        b.field("type", "half_float");
    }

    @Override
    protected Number randomNumber() {
        return randomBoolean() ? randomFloat() : randomDoubleBetween(-65504, 65504, true);
    }

    @Override
    protected SyntheticSourceSupport syntheticSourceSupport(boolean ignoreMalformed) {
        return new NumberSyntheticSourceSupport(
            n -> HalfFloatPoint.sortableShortToHalfFloat(HalfFloatPoint.halfFloatToSortableShort(n.floatValue())),
            ignoreMalformed
        );
    }

    @Override
    protected SyntheticSourceSupport syntheticSourceSupportForKeepTests(boolean ignoreMalformed, Mapper.SourceKeepMode sourceKeepMode) {
        return new NumberSyntheticSourceSupportForKeepTests(
            n -> HalfFloatPoint.sortableShortToHalfFloat(HalfFloatPoint.halfFloatToSortableShort(n.floatValue())),
            ignoreMalformed,
            sourceKeepMode
        );

    }

    @Override
    protected IngestScriptSupport ingestScriptSupport() {
        throw new AssumptionViolatedException("not supported");
    }
}
