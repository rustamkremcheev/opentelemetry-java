/*
 * Copyright 2020, OpenTelemetry Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.opentelemetry.trace.propagation;

import io.grpc.Context;
import io.opentelemetry.context.propagation.HttpTextFormat.Setter;
import io.opentelemetry.trace.DefaultSpan;
import io.opentelemetry.trace.SpanContext;
import io.opentelemetry.trace.SpanId;
import io.opentelemetry.trace.TraceFlags;
import io.opentelemetry.trace.TraceId;
import io.opentelemetry.trace.TraceState;
import io.opentelemetry.trace.TracingContextUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

@State(Scope.Thread)
public class HttpTraceContextInjectBenchmark {

  private static final List<SpanContext> spanContexts =
      Arrays.asList(
          createTestSpanContext("905734c59b913b4a905734c59b913b4a", "9909983295041501"),
          createTestSpanContext("21196a77f299580e21196a77f299580e", "993a97ee3691eb26"),
          createTestSpanContext("2e7d0ad2390617702e7d0ad239061770", "d49582a2de984b86"),
          createTestSpanContext("905734c59b913b4a905734c59b913b4a", "776ff807b787538a"),
          createTestSpanContext("68ec932c33b3f2ee68ec932c33b3f2ee", "68ec932c33b3f2ee"));
  private static final int COUNT = 5; // spanContexts.size()
  private final HttpTraceContext httpTraceContext = new HttpTraceContext();
  private final Map<String, String> carrier = new HashMap<>();
  private final Setter<Map<String, String>> setter =
      new Setter<Map<String, String>>() {
        @Override
        public void set(Map<String, String> carrier, String key, String value) {
          carrier.put(key, value);
        }
      };
  private final List<Context> contexts = createContexts(spanContexts);

  /** Benchmark for measuring inject with default trace state and sampled trace options. */
  @Benchmark
  @BenchmarkMode({Mode.AverageTime})
  @Fork(1)
  @Measurement(iterations = 15, time = 1)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  @Warmup(iterations = 5, time = 1)
  @OperationsPerInvocation(COUNT)
  public Map<String, String> measureInject() {
    for (int i = 0; i < COUNT; i++) {
      httpTraceContext.inject(contexts.get(i), carrier, setter);
    }
    return carrier;
  }

  private static SpanContext createTestSpanContext(String traceId, String spanId) {
    byte sampledTraceOptionsBytes = 1;
    TraceFlags sampledTraceOptions = TraceFlags.fromByte(sampledTraceOptionsBytes);
    TraceState traceStateDefault = TraceState.builder().build();
    return SpanContext.create(
        TraceId.fromLowerBase16(traceId, 0),
        SpanId.fromLowerBase16(spanId, 0),
        sampledTraceOptions,
        traceStateDefault);
  }

  private static List<Context> createContexts(List<SpanContext> spanContexts) {
    List<Context> contexts = new ArrayList<>();
    for (SpanContext context : spanContexts) {
      contexts.add(TracingContextUtils.withSpan(DefaultSpan.create(context), Context.ROOT));
    }
    return contexts;
  }
}
