#!/usr/bin/env python
from data.user import User
from data.base import Base, engine
from sqlalchemy.orm import sessionmaker

print("engine: {0}".format(engine))
print("table: {0}".format(User.__table__))

Base.metadata.create_all(engine)
print("Created tables.")

# Check user created
Session_class = sessionmaker(bind=engine)
session = Session_class()
# perf user should be already created
try:
    users = session.query(User).filter(User.name.in_(["perf"])).all()
except:
    users = []
if len(users) == 0:
    # create perf user
    perf_user = User(name='perf', fullname='perf', password='perf')
    session.add(perf_user)
    session.commit()
