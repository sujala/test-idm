#! /usr/bin/python
from sqlalchemy.orm import sessionmaker
from datetime import datetime

from data.base import engine
from data.machine import Machine
from data.treatment import Treatment
from data.value import Value
from data.run import Run
import json


class MachineRepository(object):
    '''
    Wraps data integration with machine model
    '''
    def __init__(self, logger):
        self.logger = logger

    def list_machines(self):
        session_class = sessionmaker(bind=engine)
        session = session_class()
        agents = session.query(Machine).all()
        return [agent.to_dict() for agent in agents]

    def get_machine(self, agent_id):
        session_class = sessionmaker(bind=engine)
        session = session_class()
        agent = session.query(Machine).filter(
            Machine.id == agent_id).one_or_none()
        if agent is not None:
            return agent.to_dict()
        else:
            return agent        

    def add_machine(self, address, main_directory):
        '''
        Registers an agent
        '''
        session_class = sessionmaker(bind=engine)
        session = session_class()

        machine = Machine(address=address,
                        main_directory=main_directory,
                        registered=datetime.now(),
                        is_up=True)
        session.add(machine)
        session.commit()

        return machine.to_dict()

    def delete_machine(self, machine_id):
        '''
        Deletes an agent
        '''
        session_class = sessionmaker(bind=engine)
        session = session_class()

        agents_to_remove = session.query(Machine).filter(
            Machine.id == machine_id).delete()
        session.commit()
        if agents_to_remove != 1:
            return False
        else:
            return True
