#!/usr/bin/env python
from sqlalchemy import (Sequence, Column, Integer, ForeignKey, String)
from data.base import Base
from sqlalchemy.orm import relationship


class Run(Base):
    __tablename__ = 'run'
    id = Column(Integer, Sequence('run_id_seq'), primary_key=True)
    treatment_id = Column(Integer, ForeignKey('treatment.id'))
    treatment = relationship("Treatment", back_populates="runs")
    name = Column(String(80))
    priority = Column(Integer)
    status = Column(String(80))

    @classmethod
    def from_treatment(cls, treatment, name):
        run = Run(priority=100, status="NotStarted")
        run.treatment = treatment
        run.name = name
        return run

    def __repr__(self):
        return ("<Run(treatment='%s', "
                "priority='%s', name='%s')>" % (self.treatment,
                                                self.priority, self.name))

    def to_dict(self):
        return {
            "id": self.id,
            "name": self.name,
            "priority": self.priority,
            "status": self.status
        }
