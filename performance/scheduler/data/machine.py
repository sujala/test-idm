#!/usr/bin/env python
from sqlalchemy import (Sequence, Column, Integer, String, DateTime, Boolean)
from .base import Base


class Machine(Base):
    __tablename__ = 'machine'
    id = Column(Integer, Sequence('machine_id_seq'), primary_key=True)
    address = Column(String(100))
    role = Column(String(50))
    main_directory = Column(String(300))
    registered = Column(DateTime)
    available = Column(Boolean)
    is_up = Column(Boolean)

    def __repr__(self):
        rep = ("<Machine(address='%s', role='%s', main_directory='%s')>" % (
               self.address, self.role, self.main_directory))
        return rep

    def to_dict(self):
        return {
            "id": self.id,
            "address": self.address,
            "role": self.role,
            "main_directory": self.main_directory,
            "registered": self.registered,
            "available": self.available,
            "is_up": self.is_up
        }
