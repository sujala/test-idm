package testHelpers;

/**
 */
public interface MultiStageTaskFactory<T extends MultiStageTask> {
    T createTask();
}
