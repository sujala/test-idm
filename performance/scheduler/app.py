#! /usr/bin/python
'''
Main entry point and router bootstrap for scheduler service

Scheduler service is responsible for the following:
    * Create an experiment (performance test profile)
    * Register and deregister a performance agent machine
    * Create an execution against an experiment
    * View all experiments and executions
'''
import logging

import falcon

from controllers import runs, experiments, agents

logging.basicConfig(format='%(levelname)s:%(message)s', level=logging.INFO)
LOGGER = logging.getLogger(__name__)
LOGGER.setLevel(logging.INFO)

api = appplication = falcon.API()
api.req_options.auto_parse_form_urlencoded = False

api.add_route('/runs', runs.Collection(LOGGER))
api.add_route('/runs/{run_id}', runs.Item(LOGGER))
api.add_route('/runs/{run_id}/actions/{action}', runs.ActionItem(LOGGER))
api.add_route('/experiments', experiments.Collection(LOGGER))
api.add_route('/experiments/{experiment_id}', experiments.Item(LOGGER))
api.add_route('/agents', agents.Collection(LOGGER))
api.add_route('/agents/{agent_id}', agents.Item(LOGGER))
