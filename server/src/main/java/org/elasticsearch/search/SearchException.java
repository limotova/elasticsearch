/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the "Elastic License
 * 2.0", the "GNU Affero General Public License v3.0 only", and the "Server Side
 * Public License v 1"; you may not use this file except in compliance with, at
 * your election, the "Elastic License 2.0", the "GNU Affero General Public
 * License v3.0 only", or the "Server Side Public License, v 1".
 */

package org.elasticsearch.search;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.ElasticsearchWrapperException;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

import java.io.IOException;
import java.util.Objects;

public class SearchException extends ElasticsearchException implements ElasticsearchWrapperException {

    private final SearchShardTarget shardTarget;

    /**
     * Creates a new instance of {@link SearchException}. To be used for subclasses that don't make a root cause available.
     * It is highly recommended to override {@link ElasticsearchException#status()} in such cases, otherwise the status code will be 500.
     */
    protected SearchException(SearchShardTarget shardTarget, String msg) {
        super(msg);
        this.shardTarget = shardTarget;
    }

    public SearchException(SearchShardTarget shardTarget, String msg, Throwable cause) {
        super(msg, Objects.requireNonNull(cause, "cause must not be null"));
        this.shardTarget = shardTarget;
    }

    public SearchException(StreamInput in) throws IOException {
        super(in);
        if (in.readBoolean()) {
            shardTarget = new SearchShardTarget(in);
        } else {
            shardTarget = null;
        }
    }

    @Override
    protected void writeTo(StreamOutput out, Writer<Throwable> nestedExceptionsWriter) throws IOException {
        super.writeTo(out, nestedExceptionsWriter);
        if (shardTarget == null) {
            out.writeBoolean(false);
        } else {
            out.writeBoolean(true);
            shardTarget.writeTo(out);
        }
    }

    public SearchShardTarget shard() {
        return this.shardTarget;
    }
}
