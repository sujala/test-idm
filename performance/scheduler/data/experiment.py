#!/usr/bin/env python
from sqlalchemy import (Sequence, Column, Integer, String)
from sqlalchemy.orm import relationship, backref
from .experiment_treatment import ExperimentTreatment
from .base import Base

import json


class Experiment(Base):
    __tablename__ = 'experiment'
    id = Column(Integer, Sequence('experiment_id_seq'), primary_key=True)
    name = Column(String(50), unique=True)
    simulation = Column(String(200))
    treatments = relationship("Treatment", secondary=ExperimentTreatment,
                              backref=backref('Experiment'))

    def __repr__(self):

        rep = ("<Experiment(name='%s', treatment='%s')>" % (self.name,
                                                            self.treatments))
        return rep

    def to_dict(self):
        return {
            "id": self.id,
            "name": self.name,
            "simulation": self.simulation,
            "treatments": [
                treatment.to_dict() for treatment in self.treatments
            ]
        }
