import json
import sys
import traceback

import falcon
from datetime import datetime, date
import decimal

from repositories import agents
import constants


def alchemyencoder(obj):
    """JSON encoder function for SQLAlchemy special classes."""
    if isinstance(obj, (datetime, date)):
        return obj.isoformat()
    elif isinstance(obj, decimal.Decimal):
        return float(obj)


class Collection(object):
    '''
    API entrypoint for /agents.
    '''

    def __init__(self, logger):
        self.logger = logger

    def on_get(self, req, resp):
        '''
        Lists all agent machines
        '''
        result = agents.MachineRepository(
            self.logger).list_machines()
        resp.status = falcon.HTTP_200
        resp.body = json.dumps(result, default=alchemyencoder)

    def on_post(self, req, resp):
        '''
        Register an agent with scheduler
        '''
        body = req.stream.read()
        if body is None:
            raise falcon.HTTPBadRequest(
                    constants.BAD_REQUEST_MISSING_ELEMENT.format(
                        'information'))
        json_body = json.loads(body.decode('utf-8'))
        if 'agent_address' not in json_body:
            raise falcon.HTTPBadRequest(
                    constants.BAD_REQUEST_MISSING_ELEMENT.format(
                        'agent_address'))
        if 'main_directory' not in json_body:
            raise falcon.HTTPBadRequest(
                    constants.BAD_REQUEST_MISSING_ELEMENT.format(
                        'main_directory'))
        result = agents.MachineRepository(
            self.logger).add_machine(json_body['agent_address'], json_body['main_directory'])
        if result is not None:
            resp.status = falcon.HTTP_CREATED
            resp.body = json.dumps(result, default=alchemyencoder)
        else:
            self.logger.error("Unable to register agent {}".format(json_body))
            resp.status = falcon.HTTP_500
            resp.body = json.dumps(
                {
                    "result": "Unable to register agent {}".format(json_body)
                })


class Item(object):
    '''
    API entrypoint for /agents/{agent_id}
    '''

    def __init__(self, logger):
        self.logger = logger

    def on_get(self, req, resp, agent_id):
        '''
        List specific agent machine
        '''
        self.logger.info("agent id: {}".format(agent_id))

        result = agents.MachineRepository(
            self.logger).get_machine(agent_id)
        if result is None:
            resp.status = falcon.HTTP_404
            resp.body = json.dumps(
                    {
                        "result": "No agents found"
                    })
        else:
            resp.body = json.dumps(result)

    def on_delete(self, req, resp, agent_id):
        '''
        Remove a registered agent
        '''
        if agents.MachineRepository(
        self.logger).delete_machine(agent_id):
            resp.status = falcon.HTTP_204
        else:
            resp.status = falcon.HTTP_INTERNAL_SERVER_ERROR
            resp.body = json.dumps(
                    {
                        "result": "Unable to deregister the agent"
                    })
