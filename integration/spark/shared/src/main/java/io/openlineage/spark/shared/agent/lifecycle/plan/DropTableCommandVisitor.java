/* SPDX-License-Identifier: Apache-2.0 */

package io.openlineage.spark.shared.agent.lifecycle.plan;

import io.openlineage.client.OpenLineage;
import io.openlineage.spark.shared.agent.util.DatasetIdentifier;
import io.openlineage.spark.shared.agent.util.PathUtils;
import io.openlineage.spark.shared.agent.util.PlanUtils;
import io.openlineage.spark.shared.api.DatasetFactory;
import io.openlineage.spark.shared.api.OpenLineageContext;
import io.openlineage.spark.shared.api.QueryPlanVisitor;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.catalyst.catalog.CatalogTable;
import org.apache.spark.sql.catalyst.plans.logical.LogicalPlan;
import org.apache.spark.sql.execution.command.DropTableCommand;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * {@link LogicalPlan} visitor that matches an {@link DropTableCommand} and extracts the output
 * {@link OpenLineage.Dataset} being written.
 */
@Slf4j
public class DropTableCommandVisitor
    extends QueryPlanVisitor<DropTableCommand, OpenLineage.OutputDataset> {

  public DropTableCommandVisitor(OpenLineageContext context) {
    super(context);
  }

  @Override
  public List<OpenLineage.OutputDataset> apply(LogicalPlan x) {
    DropTableCommand command = (DropTableCommand) x;
    Optional<CatalogTable> table = catalogTableFor(command.tableName());
    if (table.isPresent()) {
      DatasetIdentifier datasetIdentifier = PathUtils.fromCatalogTable(table.get());

      DatasetFactory<OpenLineage.OutputDataset> factory = outputDataset();
      return Collections.singletonList(
          factory.getDataset(
              datasetIdentifier,
              new OpenLineage.DatasetFacetsBuilder()
                  .schema(null)
                  .dataSource(
                      PlanUtils.datasourceFacet(
                          context.getOpenLineage(), datasetIdentifier.getNamespace()))
                  .lifecycleStateChange(
                      context
                          .getOpenLineage()
                          .newLifecycleStateChangeDatasetFacet(
                              OpenLineage.LifecycleStateChangeDatasetFacet.LifecycleStateChange
                                  .DROP,
                              null))
                  .build()));

    } else {
      // already deleted, do nothing
      return Collections.emptyList();
    }
  }
}