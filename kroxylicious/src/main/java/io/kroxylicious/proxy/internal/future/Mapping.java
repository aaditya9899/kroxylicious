/*
 * Copyright (c) 2011-2020 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package io.kroxylicious.proxy.internal.future;

import java.util.function.Function;

/**
 * Function map transformation.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
class Mapping<T, U> extends Operation<U> implements Listener<T> {

    private final Function<T, U> successMapper;

    Mapping(Function<T, U> successMapper) {
        super();
        this.successMapper = successMapper;
    }

    @Override
    public void onSuccess(T value) {
        U result;
        try {
            result = successMapper.apply(value);
        }
        catch (Throwable e) {
            tryFail(e);
            return;
        }
        tryComplete(result);
    }

    @Override
    public void onFailure(Throwable failure) {
        tryFail(failure);
    }
}
