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
package com.redhat.lightblue.util.metrics;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.redhat.lightblue.util.Error;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class DropwizardRequestMetricsTest {
    // Use fresh registry for each test
    private MetricRegistry metricsRegistry = new MetricRegistry();
    private RequestMetrics requestMetrics = new DropwizardRequestMetrics(metricsRegistry);

    @Test
    public void testStartRequestMonitoring() {
        requestMetrics.startCrudRequest("save", "name", null);
        Assert.assertNotNull(metricsRegistry.getCounters());
        Assert.assertNotNull(metricsRegistry.getTimers());

        Counter activeRequestCounter = metricsRegistry.counter("request.crud.save.name.default.active");
        Timer completedRequestTimer = metricsRegistry.timer("request.crud.save.name.default");

        Assert.assertEquals(1, activeRequestCounter.getCount());
        Assert.assertEquals(0, completedRequestTimer.getCount());
    }
    
    @Test
    public void testEndRequestMonitoring() {
        DropwizardRequestMetrics.Context context = requestMetrics.startCrudRequest("explain", "name", "1.0.0");
        context.endRequestMonitoring();

        Counter activeRequestCounter = metricsRegistry.counter("request.crud.explain.name.1_0_0.active");
        Timer completedRequestTimer = metricsRegistry.timer("request.crud.explain.name.1_0_0");

        Assert.assertEquals(0, activeRequestCounter.getCount());
        Assert.assertEquals(1, completedRequestTimer.getCount());
    }

    @Test
    public void testStartRequestMonitoringNullVersion() {
        DropwizardRequestMetrics.Context context = requestMetrics.startCrudRequest("delete", "name", "1.2.3");
        Assert.assertNotNull(metricsRegistry.getCounters());
        Assert.assertNotNull(metricsRegistry.getTimers());

        Counter activeRequestCounter = metricsRegistry.counter("request.crud.delete.name.1_2_3.active");
        Timer completedRequestTimer = metricsRegistry.timer("request.crud.delete.name.1_2_3");
        Assert.assertEquals(1, activeRequestCounter.getCount());
        Assert.assertEquals(0, completedRequestTimer.getCount());
        
        context.endRequestMonitoring();
        Assert.assertEquals(1, completedRequestTimer.getCount());
    }
    
    @Test
    public void testMarkRequestExceptionWithException() {
        DropwizardRequestMetrics.Context context = requestMetrics.startBulkRequest();
        context.markRequestException(new NullPointerException());
        Meter exceptionMeter = metricsRegistry.meter("request.bulk.errors.java_lang_NullPointerException");
        Assert.assertEquals(1, exceptionMeter.getCount());
    }
    
    @Test
    public void testMarkRequestExceptionWithError() {
        DropwizardRequestMetrics.Context errorContext1 = requestMetrics.startLockRequest("lockcommand", "domain");
        errorContext1.markRequestException(Error.get("mongo-crud:SaveError:InsertionAttemptWithNoUpsert"));
        Meter exceptionMeter1 = metricsRegistry.meter("request.lock.domain.lockcommand.errors.mongo-crud_SaveError_InsertionAttemptWithNoUpsert");
        Assert.assertEquals(1, exceptionMeter1.getCount());

        DropwizardRequestMetrics.Context errorContext2 = requestMetrics.startCrudRequest("find", "name", null);
        errorContext2.markRequestException(Error.get("mongo-crud:SaveClobblersHiddenFields"));
        Meter exceptionMeter2 = metricsRegistry.meter("request.crud.find.name.default.errors.mongo-crud_SaveClobblersHiddenFields");
        Assert.assertEquals(1, exceptionMeter2.getCount());
    }
    
    @Test
    public void testMarkRequestExceptionWithWeirdCharactersAreEscaped() {
        DropwizardRequestMetrics.Context errorContext1 = requestMetrics.startLockRequest("lockcommand", "domain");
        errorContext1.markRequestException(Error.get("(just-checking@some#regex!replace$in%action^)"));
        Meter exceptionMeter1 = metricsRegistry.meter("request.lock.domain.lockcommand.errors._just-checking_some_regex_replace_in_action__");
        Assert.assertEquals(1, exceptionMeter1.getCount());

        DropwizardRequestMetrics.Context errorContext2 = requestMetrics.startCrudRequest("find", "name", null);
        errorContext2.markRequestException(Error.get("{testing&some*more+regex~replace=in;action'}"));
        Meter exceptionMeter2 = metricsRegistry.meter("request.crud.find.name.default.errors._testing_some_more_regex_replace_in_action__");
        Assert.assertEquals(1, exceptionMeter2.getCount());

        DropwizardRequestMetrics.Context errorContext3 = requestMetrics.startCrudRequest("delete", "name", "version");
        errorContext3.markRequestException(Error.get("\\yet<more>regex?replace,action\"in here\\"));
        Meter exceptionMeter3 = metricsRegistry.meter("request.crud.delete.name.version.errors._yet_more_regex_replace_action_in_here_");
        Assert.assertEquals(1, exceptionMeter3.getCount());
    }
    
    @Test
    public void testmarkAllErrorsAndEndRequestMonitoring() {
        DropwizardRequestMetrics.Context context = requestMetrics.startSavedSearchRequest("find", "name", "version");

        List<Error> errors = new ArrayList<>();
        Error error1 =  Error.get("rest-crud:SavedSearchError");
        Error error2 =  Error.get("mongo-crud:DatabaseError");
        errors.add(error1);
        errors.add(error2);

        context.markAllErrorsAndEndRequestMonitoring(errors);

        Counter activeRequestCounter = metricsRegistry.counter("request.saved-search.name.find.version.active");
        Timer completedRequestTimer = metricsRegistry.timer("request.saved-search.name.find.version");
        Meter restExceptionMeter = metricsRegistry.meter("request.saved-search.name.find.version.errors.rest-crud_SavedSearchError");
        Meter mongoExceptionMeter = metricsRegistry.meter("request.saved-search.name.find.version.errors.mongo-crud_DatabaseError");

        Assert.assertEquals(0, activeRequestCounter.getCount());
        Assert.assertEquals(1, completedRequestTimer.getCount());
        Assert.assertEquals(1, restExceptionMeter.getCount());
        Assert.assertEquals(1, mongoExceptionMeter.getCount());
    }    
}

