package io.openlineage.flink.agent.lifecycle;

import io.openlineage.client.OpenLineage;
import io.openlineage.client.OpenLineage.RunEvent;
import io.openlineage.client.OpenLineage.RunEvent.EventType;
import io.openlineage.flink.SinkLineage;
import io.openlineage.flink.TransformationUtils;
import io.openlineage.flink.agent.EventEmitter;
import io.openlineage.flink.agent.client.OpenLineageClient;
import io.openlineage.flink.agent.facets.CheckpointFacet;
import io.openlineage.flink.api.OpenLineageContext;
import io.openlineage.flink.visitor.Visitor;
import io.openlineage.flink.visitor.VisitorFactory;
import io.openlineage.flink.visitor.VisitorFactoryImpl;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.JobExecutionResult;
import org.apache.flink.api.common.JobID;
import org.apache.flink.api.dag.Transformation;

@Slf4j
public class FlinkExecutionContext implements ExecutionContext {

  @Getter private final JobID jobId;
  private final EventEmitter eventEmitter;
  private final OpenLineageContext openLineageContext;

  @Getter private final List<Transformation<?>> transformations;

  public FlinkExecutionContext(
      JobID jobId, EventEmitter eventEmitter, List<Transformation<?>> transformations) {
    this.jobId = jobId;
    this.eventEmitter = eventEmitter;
    this.transformations = transformations;
    this.openLineageContext =
        OpenLineageContext.builder()
            .openLineage(new OpenLineage(OpenLineageClient.OPEN_LINEAGE_CLIENT_URI))
            .build();
  }

  @Override
  public void onJobSubmitted() {
    log.debug("JobClient - jobId: {}", jobId);
    RunEvent runEvent = buildEventForEventType(EventType.START).build();
    log.debug("Posting event for onJobSubmitted {}: {}", jobId, runEvent);
    eventEmitter.emit(runEvent);
  }

  @Override
  public void onJobCheckpoint(CheckpointFacet facet) {
    log.debug("JobClient - jobId: {}", jobId);
    RunEvent runEvent =
        buildEventForEventType(EventType.OTHER)
            .run(
                new OpenLineage.RunBuilder()
                    .facets(new OpenLineage.RunFacetsBuilder().put("checkpoints", facet).build())
                    .build())
            .build();
    // TODO: introduce better event type than OTHER
    log.debug("Posting event for onJobCheckpoint {}: {}", jobId, runEvent);
    eventEmitter.emit(runEvent);
  }

  public OpenLineage.RunEventBuilder buildEventForEventType(EventType eventType) {
    TransformationUtils converter = new TransformationUtils();
    List<SinkLineage> sinkLineages = converter.convertToVisitable(transformations);

    VisitorFactory visitorFactory = new VisitorFactoryImpl();
    List<OpenLineage.InputDataset> inputDatasets = new ArrayList<>();
    List<OpenLineage.OutputDataset> outputDatasets = new ArrayList<>();

    for (var lineage : sinkLineages) {
      inputDatasets.addAll(getInputDatasets(visitorFactory, lineage.getSources()));
      outputDatasets.addAll(getOutputDatasets(visitorFactory, lineage.getSink()));
    }

    return openLineageContext
        .getOpenLineage()
        .newRunEventBuilder()
        .inputs(inputDatasets)
        .outputs(outputDatasets)
        .eventType(eventType);
  }

  @Override
  public void onJobExecuted(JobExecutionResult jobExecutionResult) {}

  private List<OpenLineage.InputDataset> getInputDatasets(
      VisitorFactory visitorFactory, List<Object> sources) {
    List<OpenLineage.InputDataset> inputDatasets = new ArrayList<>();
    List<Visitor<OpenLineage.InputDataset>> inputVisitors =
        visitorFactory.getInputVisitors(openLineageContext);

    for (var transformation : sources) {
      inputDatasets.addAll(
          inputVisitors.stream()
              .filter(inputVisitor -> inputVisitor.isDefinedAt(transformation))
              .map(inputVisitor -> inputVisitor.apply(transformation))
              .flatMap(List::stream)
              .collect(Collectors.toList()));
    }
    return inputDatasets;
  }

  private List<OpenLineage.OutputDataset> getOutputDatasets(
      VisitorFactory visitorFactory, Object sink) {
    List<Visitor<OpenLineage.OutputDataset>> outputVisitors =
        visitorFactory.getOutputVisitors(openLineageContext);

    return outputVisitors.stream()
        .filter(inputVisitor -> inputVisitor.isDefinedAt(sink))
        .map(inputVisitor -> inputVisitor.apply(sink))
        .flatMap(List::stream)
        .collect(Collectors.toList());
  }
}
