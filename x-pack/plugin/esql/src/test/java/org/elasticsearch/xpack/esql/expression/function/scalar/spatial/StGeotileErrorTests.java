/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.esql.expression.function.scalar.spatial;

import org.elasticsearch.xpack.esql.core.expression.Expression;
import org.elasticsearch.xpack.esql.core.tree.Source;
import org.elasticsearch.xpack.esql.core.type.DataType;
import org.elasticsearch.xpack.esql.expression.function.ErrorsForCasesWithoutExamplesTestCase;
import org.elasticsearch.xpack.esql.expression.function.TestCaseSupplier;
import org.hamcrest.Matcher;

import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.equalTo;

public class StGeotileErrorTests extends ErrorsForCasesWithoutExamplesTestCase {
    @Override
    protected List<TestCaseSupplier> cases() {
        return paramsToSuppliers(StGeotileTests.parameters());
    }

    @Override
    protected Expression build(Source source, List<Expression> args) {
        return new StGeotile(source, args.get(0), args.get(1), args.size() > 2 ? args.get(2) : null);
    }

    @Override
    protected Matcher<String> expectedTypeErrorMatcher(List<Set<DataType>> validPerPosition, List<DataType> signature) {
        return equalTo(typeErrorMessage(true, validPerPosition, signature, (v, p) -> switch (p) {
            case 0 -> "geo_point";
            case 1 -> "integer";
            case 2 -> "geo_shape";
            default -> throw new IllegalStateException("Unexpected value: " + p);
        }));
    }
}
