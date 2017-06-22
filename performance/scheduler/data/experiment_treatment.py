#!/usr/bin/env python
from sqlalchemy import (Column, Integer, ForeignKey, Table)
from .base import Base

ExperimentTreatment = Table(
    'experimenttreatment', Base.metadata,
    Column("treatment_id", Integer, ForeignKey('treatment.id')),
    Column("experiment_id", Integer, ForeignKey('experiment.id')))
