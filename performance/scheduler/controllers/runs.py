import json
import sys
import traceback

import falcon

import constants
from repositories import runs


class Collection(object):
    '''
    API entrypoint for /runs.
    '''

    def __init__(self, logger):
        self.logger = logger

    def on_get(self, req, resp):
        '''
        Lists all runs
        '''
        perf_runs = runs.RunRepository(
            self.logger).get_run_list()
        resp.body = json.dumps(perf_runs)

    def on_post(self, req, resp):
        '''
        Add a run to the database.  This will then be exposed
        via another api call to start a test
        '''
        body = req.stream.read()
        if body is None:
            raise falcon.HTTPBadRequest(
                    constants.BAD_REQUEST_MISSING_ELEMENT.format(
                        'data'))
        json_body = json.loads(body.decode('utf-8'))
        self.logger.info("request to start run: {}".format(json_body))
        if 'run_names' not in json_body:
            raise falcon.HTTPBadRequest(
                    constants.BAD_REQUEST_MISSING_ELEMENT.format(
                        'run_names'))
        if 'experiment_id' not in json_body:
            raise falcon.HTTPBadRequest(
                    constants.BAD_REQUEST_MISSING_ELEMENT.format(
                        'experiment_id'))

        results = runs.RunRepository(
            self.logger).schedule_run(experiment_id=json_body['experiment_id'],
                                      run_names=json_body['run_names'])
        if results is None:
            self.logger.error("Unable to start {}".format(
                json_body['experiment_id']))
            resp.status = falcon.HTTP_500
            resp.body = json.dumps(
                {
                    "result": "We could not start experiment {}".format(
                        json_body['experiment_id'])})
        else:
            resp.body = json.dumps(results)
            resp.status = falcon.HTTP_201


class Item(object):
    '''
    API entrypoint for /runs/{run_id}.
    '''

    def __init__(self, logger):
        self.logger = logger

    def on_get(self, req, resp, run_id):
        '''
        Lists all runs
        '''
        self.logger.info("run id: {}".format(run_id))

        perf_run = runs.RunRepository(
            self.logger).get_run(run_id)
        if perf_run is None:
            resp.status = falcon.HTTP_404
            resp.body = json.dumps(
                    {
                        "result": "No runs found"
                    })
        else:
            resp.body = json.dumps(perf_run)

class ActionItem(object):
    '''
    API entrypoint for /runs/{run_id}/actions/{action_id}.
    '''
    def __init__(self, logger):
        self.logger = logger

    def on_get(self, req, resp, run_id, action):
        '''
        Start and stop runs
        '''
        self.logger.info("action: {} for {}".format(action, run_id))
        resp.status = falcon.HTTP_202
        if action == "start":
            perf_run = runs.RunRepository(
                self.logger).update_run(run_id, status="Running")
            if perf_run is not None:
                resp.body = json.dumps(perf_run)
            else:
                resp.status = falcon.HTTP_INTERNAL_SERVER_ERROR
                resp.body = json.dumps(
                        {
                            "result": "Unable to start {}".format(run_id)
                        })
        elif action == "stop":
            perf_run = runs.RunRepository(
                self.logger).update_run(run_id, status="Completed")
            if perf_run is not None:
                resp.body = json.dumps(perf_run)
            else:
                resp.status = falcon.HTTP_INTERNAL_SERVER_ERROR
                resp.body = json.dumps(
                        {
                            "result": "Unable to stop {}".format(run_id)
                        })
        else:
            resp.status = falcon.HTTP_400
            resp.body = json.dumps(
                    {
                        "results": "Invalid action: {}".format(action)
                    })
