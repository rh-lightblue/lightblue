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

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import java.util.stream.Stream;
import java.util.stream.Collectors;

import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import com.redhat.lightblue.query.QueryExpression;
import com.redhat.lightblue.query.NaryLogicalOperator;

public class Assemble extends Step<ResultDocument> {

    private static final Logger LOGGER=LoggerFactory.getLogger(Assemble.class);

    private final ExecutionBlock[] destinationBlocks;
    private final Step<ResultDocument> source;
    private Map<ExecutionBlock,Assemble> destinations;
    
    public Assemble(ExecutionBlock block,
                    ExecutionBlock[] destinationBlocks) {
        super(block);
        this.source=block.getResultStep();
        this.destinationBlocks=destinationBlocks;
    }

    public List<ResultDocument> getResultList(QueryExpression q,ExecutionContext ctx) {
        if(source instanceof Retrieve) {
            ((Retrieve)source).setQuery(q);
            StepResult<ResultDocument> results=source.getResults(ctx);
            return results.stream().collect(Collectors.toList());
        } else
            throw new IllegalStateException("Source must have been an instance of Retrieve, but "+
                                            source.getClass().getName());
    }

    @Override
    public StepResult<ResultDocument> getResults(ExecutionContext ctx) {
        destinations=new HashMap<ExecutionBlock,Assemble>();
        for(ExecutionBlock x:destinationBlocks) {
            Assemble a=x.getStep(Assemble.class);
            if(a!=null)
                destinations.put(x,a);
            else
                throw new IllegalArgumentException("No assemble step in "+x);
        }
        // Get the results from the source
        StepResult<ResultDocument> sourceResults=source.getResults(ctx);
        List<ResultDocument> results=sourceResults.stream().collect(Collectors.toList());

        List<Future> assemblers=new ArrayList<>();
        for(Map.Entry<ExecutionBlock,Assemble> destination:destinations.entrySet()) {
            AssociationQuery aq=destination.getKey().getAssociationQueryForEdge(block);
            BatchAssembler batchAssembler=new BatchAssembler(256,aq,destination.getValue(),ctx);
            assemblers.add(ctx.getExecutor().submit(() -> {
                        if(aq.getQuery()!=null) {
                            results.stream().forEach(batchAssembler::addDoc);
                            batchAssembler.endDoc();
                        } else {
                            results.stream().forEach(doc -> {
                                    batchAssembler.addDoc(doc);
                                    Map<ChildSlot,QueryExpression> queries=Searches.
                                        writeChildQueriesFromParentDoc(aq,doc);
                                    queries.values().stream().forEach(batchAssembler::addQuery);
                                    batchAssembler.endDoc();
                                });
                        }
                        batchAssembler.commit();
                    }));
        }
        try {
            for(Future x:assemblers)
                x.get();
        } catch (Exception ie) {
            throw new RuntimeException(ie);
        }
        // Stream results
        return new ListStepResult(results);
    }

    private static class DocAndQ {
        private final ResultDocument doc;
        private final List<QueryExpression> queries=new ArrayList<>();

        public DocAndQ(ResultDocument doc) {
            this.doc=doc;
        }
    }
    
    private static class BatchAssembler {
        private List<DocAndQ> docs=new ArrayList<>();
        private List<QueryExpression> queries=new ArrayList<>();
        private final int batchSize;
        private final AssociationQuery aq;
        private final Assemble dest;
        private final ExecutionContext ctx;

        public BatchAssembler(int batchSize,AssociationQuery aq,Assemble dest,ExecutionContext ctx) {
            this.batchSize=batchSize;
            this.dest=dest;
            this.aq=aq;
            this.ctx=ctx;
        }
        
        public void addQuery(QueryExpression q) {
            docs.get(docs.size()-1).queries.add(q);
            queries.add(q);
        }

        public void addDoc(ResultDocument doc) {
            docs.add(new DocAndQ(doc));
        }

        public void endDoc() {
            if(queries.size()>=batchSize)
                commit();
        }
        
        public void commit() {
            if(!docs.isEmpty()) {
                QueryExpression combinedQuery;
                if(!queries.isEmpty()) {
                    combinedQuery=Searches.combine(NaryLogicalOperator._or,queries);
                    LOGGER.debug("Combined retrieval query:{}",combinedQuery);
                } else {
                    combinedQuery=null;
                }
                List<ResultDocument> destResults=dest.getResultList(combinedQuery,ctx);
                for(DocAndQ parentDocAndQ:docs) {
                    Searches.associateDocs(parentDocAndQ.doc,destResults,aq);                    
                }
            }
            docs=new ArrayList<>();
            queries=new ArrayList<>();
        }
    }

    @Override
    public JsonNode toJson() {
        return null;
    }
}

