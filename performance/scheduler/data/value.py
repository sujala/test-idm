#!/usr/bin/env python
from sqlalchemy import (Sequence, Column, Integer, String)
from sqlalchemy.orm import relationship, backref
from .treatment_value import TreatmentValue
from .base import Base


class Value(Base):
    __tablename__ = 'value'
    id = Column(Integer, Sequence('value_id_seq'), primary_key=True)
    type_ = Column(String(50))
    name = Column(String(200))
    value = Column(String(500))
    treatments = relationship('Treatment', secondary=TreatmentValue,
                              backref=backref('Value'))

    def __repr__(self):
        rep = ("<Value(name='%s', value='%s', treatments='%s')>"
               "" % (self.name, self.value, self.treatments))
        return rep

    def to_dict(self):
        return {
            "id": self.id,
            "type": self.type_,
            "name": self.name,
            "value": self.value
        }
