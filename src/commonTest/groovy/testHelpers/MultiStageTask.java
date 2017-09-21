package testHelpers;

/**
 */
public interface MultiStageTask {

    /**
     * Setup code that the runner will call as soon as the task is created. Is not synchronized with any other thread.
     */
   void setup();

    /**
     * The number of concurrent stages the task uses.
     *
     * @return
     */
   int getNumberConcurrentStages();

    /**
     * Should run a particular stage of code
     *
     * @param i
     */
   void runStage(int i);

    /**
     * Once all stages have been run, this code will be run.
     */
   void cleanup();
}
