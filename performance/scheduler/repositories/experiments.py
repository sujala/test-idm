#! /usr/bin/python
from sqlalchemy.orm import sessionmaker

from data.base import engine
from data.experiment import Experiment
from data.treatment import Treatment
from data.value import Value
from data.run import Run
import json


class ExperimentRepository(object):
    '''
    Wraps data integration with experiment model
    '''
    def __init__(self, logger):
        self.logger = logger

    def add_experiment(self, application_io, name, simulation):
        '''
        Add experiment and application.properties treatment
        '''
        session_class = sessionmaker(bind=engine)
        session = session_class()

        result = session.query(Experiment).filter(Experiment.name == name).one_or_none()
        if result is not None:
            return None

        experiment = Experiment(name=name, simulation=simulation)
        treatment = Treatment(experiments=[experiment])
        session.add(experiment)
        for line in application_io:
            key, value = tuple(line.decode("utf-8").split('='))
            val = Value(type_="Average", name=key, value=value[:-1],
                        treatments=[treatment])
            session.add(val)
        session.commit()

        self.logger.info(session.query(Experiment))

        return experiment.to_dict()

    def get_experiment(self, experiment_id):
        '''
        Return specific experiment
        '''
        session_class = sessionmaker(bind=engine)
        session = session_class()
        session_class = sessionmaker(bind=engine)
        session = session_class()
        result = session.query(Experiment).filter(Experiment.id == experiment_id).one_or_none()
        if result is not None:
            return result.to_dict()
        return result

    def get_experiment_list(self):
        '''
        Return all experiments
        '''
        session_class = sessionmaker(bind=engine)
        session = session_class()
        result = session.query(Experiment).all()
        if result is not None:
            return [item.to_dict() for item in result]
        return []
