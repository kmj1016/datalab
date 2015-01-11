// DataflowCommand.java
//

package com.google.cloud.datalab.dataflow;

import java.util.*;
import java.util.logging.*;
import com.google.cloud.dataflow.sdk.*;
import ijava.extensibility.*;

public final class DataflowCommand implements Command {

  private final Shell _shell;
  private final DataflowExtension _extension;

  public DataflowCommand(Shell shell, DataflowExtension extension) {
    _shell = shell;
    _extension = extension;
  }

  private Dataflow createDataflow() throws Exception {
    Class<Dataflow> dataflowClass = Dataflow.class;

    for (String typeName: _shell.getTypeNames()) {
      Class<?> declaredClass = _shell.getType(typeName);
      if (dataflowClass.isAssignableFrom(declaredClass)) {
        try {
          return (Dataflow)declaredClass.newInstance();
        }
        catch (Exception e) {
          throw new EvaluationError("Unable to instantiate the class '" + typeName + "' " +
              "as an implementation of Dataflow.\n" +
              "Make sure it has a public parameterless constructor.");
        }
      }
    }

    throw new EvaluationError("A class extending Dataflow was not found.");
  }

  private Object runDataflow() throws Exception {
    Logger rootLogger = LogManager.getLogManager().getLogger("");
    Level loggingLevel = rootLogger.getLevel();
    try {
      // Turn off the log spew from the dataflow sdk
      rootLogger.setLevel(Level.OFF);

      return runDataflowCore();
    }
    finally {
      rootLogger.setLevel(loggingLevel);
    }
  }

  private Object runDataflowCore() throws Exception {
    _extension.setPipelineResult(null);

    ShellDataRegistry dataRegistry = new ShellDataRegistry(_shell);
    InteractivePipelineRunner runner = new InteractivePipelineRunner(dataRegistry);

    Dataflow dataflow = createDataflow();
    dataflow.initialize(dataRegistry, /* args */ null);

    Pipeline pipeline = dataflow.createPipeline(runner.getPipelineOptions());
    InteractivePipelineResult result = runner.run(pipeline);

    _extension.setPipelineResult(result);
    return result.createGraph().render();
  }

  /**
   * {@link Command}
   */
  @Override
  public Object evaluate(String arguments, String data, long evaluationID,
                         Map<String, Object> metadata) throws Exception {
    if (arguments.equals("run")) {
      return runDataflow();
    }

    throw new EvaluationError("Unknown dataflow command.");
  }
}
