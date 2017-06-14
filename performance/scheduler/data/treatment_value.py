#!/usr/bin/env python
from sqlalchemy import (Column, Integer, ForeignKey, Table)
from .base import Base

TreatmentValue = Table(
    'treatmentvalue', Base.metadata,
    Column("treatment_id", Integer, ForeignKey('treatment.id')),
    Column("value_id", Integer, ForeignKey('value.id')))
