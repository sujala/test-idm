import json
import sys
import traceback

import falcon

import constants
from repositories import experiments


class Collection(object):
    '''
    API entrypoint for /experiments.
    '''

    def __init__(self, logger):
        self.logger = logger

    def on_get(self, req, resp):
        '''
        Lists all experiments
        '''
        result = experiments.ExperimentRepository(
            self.logger).get_experiment_list()
        resp.body = json.dumps(result)


class Item(object):
    '''
    API entrypoint for /experiments/{experiment_id}
    '''

    def __init__(self, logger):
        self.logger = logger

    def on_get(self, req, resp, experiment_id):
        '''
        List specific experiment
        '''
        self.logger.info("run id: {}".format(experiment_id))

        result = experiments.ExperimentRepository(
            self.logger).get_experiment(experiment_id)
        if result is None:
            resp.status = falcon.HTTP_404
            resp.body = json.dumps(
                    {
                        "result": "No experiments found"
                    })
        else:
            resp.body = json.dumps(result)

    def on_post(self, req, resp, experiment_id):
        '''
        Create experiment with name and application.properties
        Call it curl http://127.0.0.1:8080/experiment/test \
        --data-binary @path/to/application.properties
        '''
        if 'simulation' not in req.params:
            raise falcon.HTTPBadRequest(
                    constants.BAD_REQUEST_MISSING_ELEMENT.format('simulation'))
        result = experiments.ExperimentRepository(
            self.logger).add_experiment(
                    req.stream, experiment_id, req.get_param('simulation'))
        if result is not None:
            resp.status = falcon.HTTP_CREATED
            resp.body = json.dumps(result)
        else:
            self.logger.error(
                    "Unable to create experiment {}".format(experiment_id))
            resp.status = falcon.HTTP_409
            resp.body = json.dumps(
                {
                    "result": "Experiment {} already exists".format(
                        experiment_id)
                })

