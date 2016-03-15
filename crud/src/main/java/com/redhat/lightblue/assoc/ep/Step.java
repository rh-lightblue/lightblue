/*
 Copyright 2013 Red Hat, Inc. and/or its affiliates.

 This file is part of lightblue.

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.redhat.lightblue.assoc.ep;

import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;

import com.redhat.lightblue.util.JsonUtils;

public abstract class Step<R> {

    protected final ExecutionBlock block;

    public Step(ExecutionBlock block) {
        this.block=block;
        block.registerStep(this);
    }
    
    public abstract StepResult<R> getResults(ExecutionContext ctx);

    public abstract JsonNode toJson();

    public ExecutionBlock getBlock() {
        return block;
    }

    @Override
    public String toString() {
        return JsonUtils.prettyPrint(toJson());
    }
}