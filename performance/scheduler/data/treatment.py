#!/usr/bin/env python
from sqlalchemy import (Sequence, Column, Integer)
from sqlalchemy.orm import relationship, backref
from .treatment_value import TreatmentValue
from .experiment_treatment import ExperimentTreatment
from .value import Value
from .run import Run
from .base import Base


class Treatment(Base):
    __tablename__ = 'treatment'
    id = Column(Integer, Sequence('treatment_id_seq'), primary_key=True)
    values = relationship(Value, secondary=TreatmentValue,
                          backref=backref('Treatment'))
    experiments = relationship("Experiment", secondary=ExperimentTreatment,
                               backref=backref('Treatment'))
    runs = relationship(Run, back_populates="treatment")

    def __repr__(self):
        rep = ("<Treatment(values='%s', experiments='%s', runs='%s')>"
               "" % (self.values, self.experiments, self.runs))
        return rep

    def to_dict(self):
        return {
            "id": self.id,
            "runs": [
                run.to_dict() for run in self.runs
            ],
            "values": [
                value.to_dict() for value in self.values
            ]
        }
