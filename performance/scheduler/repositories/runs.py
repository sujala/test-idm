#! /usr/bin/python
from sqlalchemy.orm import sessionmaker

from data.base import engine
from data.experiment import Experiment
from data.run import Run
import json
import collections


class RunRepository(object):
    '''
    Wraps data integration with run model
    '''
    def __init__(self, logger):
        self.logger = logger

    def schedule_run(self, experiment_id, run_names, priority=100):
        '''
        Schedule runs for performance testing
        '''
        run = None
        session_class = sessionmaker(bind=engine)
        session = session_class()
        # get experiment
        experiment = session.query(Experiment).filter(
            Experiment.id == experiment_id).one_or_none()

        if experiment is None:
            self.logger.error("Experiment {0} not found".format(experiment_id))
            return None
        i = 0
        for treatment in experiment.treatments:
            if len(run_names) == 1:
                run_name = run_names[0]
            else:
                if i >= len(run_names):
                    i = 0  # recycle
                run_name = run_names[i]
                i += 1
            run = Run.from_treatment(treatment=treatment, name=run_name)
            run.priority = priority
            session.add(run)
        session.commit()
        return collections.OrderedDict(run.to_dict())

    def get_run(self, run_id):
        '''
        Return specific run
        '''
        session_class = sessionmaker(bind=engine)
        session = session_class()
        session_class = sessionmaker(bind=engine)
        session = session_class()
        result = session.query(Run).filter(Run.id == run_id).one_or_none()
        if result is not None:
            return result.to_dict()
        return result

    def get_run_list(self):
        '''
        Return all runs
        '''
        session_class = sessionmaker(bind=engine)
        session = session_class()
        result = session.query(Run).all()
        if result is not None:
            return [item.to_dict() for item in result]
        return result

    def update_run(self, run_id, status="NotStarted"):
        '''
        Start run for performance testing
        '''
        session_class = sessionmaker(bind=engine)
        session = session_class()
        # get experiment
        run = session.query(Run).filter(
            Run.id == run_id).one_or_none()

        if run is None:
            self.logger.error("Run {0} not found".format(run_id))
            return None
        run.status = status
        session.commit()
        return run.to_dict()
