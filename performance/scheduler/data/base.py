from sqlalchemy import create_engine
from sqlalchemy.ext.declarative import declarative_base, DeclarativeMeta
from sqlalchemy.orm import sessionmaker
from marshmallow_sqlalchemy import ModelConversionError, ModelSchema
from sqlalchemy import event
from sqlalchemy.orm import mapper

import json


def setup_schema(Base, session):
    '''
    Create a function which incorporates the Base and session information
    '''
    def setup_schema_fn():
        print("in setup schema fn")
        for class_ in Base._decl_class_registry.values():
            print("in class {0}".format(class_))
            if hasattr(class_, '__tablename__'):
                if class_.__name__.endswith('Schema'):
                    raise ModelConversionError(
                        "For safety, setup_schema can not be used when a"
                        "model class ends with 'Schema'"
                    )

                class Meta(object):
                    model = class_
                    sqla_session = session

                schema_class_name = '%sSchema' % class_.__name__

                schema_class = type(
                    schema_class_name,
                    (ModelSchema,),
                    {'Meta': Meta}
                )

                setattr(class_, '__marshmallow__', schema_class)

    return setup_schema_fn


def new_alchemy_encoder():
    _visited_objs = []

    class AlchemyEncoder(json.JSONEncoder):
        def default(self, obj):
            if isinstance(obj.__class__, DeclarativeMeta):
                # don't re-visit self
                if obj in _visited_objs:
                    return None
                _visited_objs.append(obj)

                # an SQLAlchemy class
                fields = {}
                for field in [x for x in dir(obj)
                              if not x.startswith('_') and x != 'metadata']:
                    fields[field] = obj.__getattribute__(field)
                # a json-encodable dict
                return fields

            return json.JSONEncoder.default(self, obj)
    return AlchemyEncoder

engine = create_engine('sqlite:///perf.db', echo=True)

Session = sessionmaker(bind=engine)
Base = declarative_base(bind=engine)

event.listen(mapper, 'after_configured', setup_schema(Base, Session))
