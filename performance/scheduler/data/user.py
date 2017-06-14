#!/usr/bin/env python
from sqlalchemy import (Sequence, Column, Integer, String)
from .base import Base


class User(Base):
    __tablename__ = 'user'
    id = Column(Integer, Sequence('user_id_seq'), primary_key=True)
    name = Column(String(50))
    fullname = Column(String(50))
    password = Column(String(50))

    def __repr__(self):
        rep = ("<User(name='%s', fullname='%s', password='%s')>" % (
               self.name, self.fullname, self.password))
        return rep
