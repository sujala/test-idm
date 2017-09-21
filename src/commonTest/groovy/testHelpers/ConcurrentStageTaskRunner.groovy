package testHelpers

import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.User
import org.springframework.util.Assert

import java.util.concurrent.CountDownLatch

class ConcurrentStageTaskRunner {
    public <T> List<T> runConcurrent(int nThreads, MultiStageTaskFactory<T> factory) throws InterruptedException {
        Assert.isTrue(nThreads > 0, "Number of threads must be at least 1")
        Assert.notNull(factory, "A factory must be provided")

        // create the tasks - one per thread
        List<MultiStageTask> multiStageTasks = new ArrayList<MultiStageTask>(nThreads);
        for (int i = 0; i < nThreads; i++) {
            multiStageTasks.add(factory.createTask())
        }

        final int numStages = multiStageTasks.get(0).getNumberConcurrentStages();

        //create a latch for each stage
        List<CountDownLatch> stageLatches = new ArrayList<CountDownLatch>(numStages);
        for (int i = 0; i < numStages; i++) {
            stageLatches.add(new CountDownLatch(nThreads))
        }

        /**
         * The cleanup code for any particular thread must not run until all the threads have reached the clean up stage
         */
        final CountDownLatch cleanUpGate = new CountDownLatch(nThreads)

        /**
         * Have a final gate just for ensuring all cleanup code has finished and all threads are complete.
         */
        final CountDownLatch endGate = new CountDownLatch(nThreads)

        /*
        Run through the process - setup, stages, cleanup, end
         */
        for (int i = 0; i < nThreads; i++) {
            final MultiStageTask stagedTask = multiStageTasks.get(i)
            Thread t = new Thread() {
                public void run() {
                    try {
                        try {
                            stagedTask.setup();
                            for (int stage = 0; stage < numStages; stage++) {
                                CountDownLatch stageLatch = stageLatches.get(stage)
                                stageLatch.countDown()
                                stageLatch.await()
                                stagedTask.runStage(stage + 1)
                            }
                        }
                        finally {
                            cleanUpGate.countDown()
                            cleanUpGate.await()
                            try {
                                stagedTask.cleanup()
                            } catch (Exception ex) {
                                //eat.
                            }
                            endGate.countDown()
                        }
                    }
                    catch (InterruptedException ex) {
                        //eat
                    }
                }
            };
            t.start()
        }
        endGate.await()
        return multiStageTasks;
    }
}
