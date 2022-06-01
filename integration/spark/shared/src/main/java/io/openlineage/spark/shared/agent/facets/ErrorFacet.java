/* SPDX-License-Identifier: Apache-2.0 */

package io.openlineage.spark.shared.agent.facets;

import io.openlineage.client.OpenLineage;
import io.openlineage.spark.shared.agent.Versions;
import lombok.Builder;
import lombok.NonNull;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ErrorFacet extends OpenLineage.DefaultRunFacet {
  private final Exception exception;

  @Builder
  public ErrorFacet(@NonNull Exception exception) {
    super(Versions.OPEN_LINEAGE_PRODUCER_URI);
    this.exception = exception;
  }

  public String getMessage() {
    return exception.getMessage();
  }

  public String getStackTrace() {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    exception.printStackTrace(pw);
    return sw.toString();
  }
}